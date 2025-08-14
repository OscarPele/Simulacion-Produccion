package com.oscar.proyecto.ms_auth.token;

import jakarta.transaction.Transactional;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.List;

@Service
@Transactional
public class RefreshTokenService {

    private final RefreshTokenRepository repository;
    private final int refreshDays;
    private final int maxSessionsPerUser;
    private final boolean persistPlaintext; // <-- flag compat
    private final SecureRandom secureRandom = new SecureRandom();

    public RefreshTokenService(
            RefreshTokenRepository repository,
            @Value("${app.jwt.refresh-expiration-days:7}") int refreshDays,
            @Value("${app.jwt.max-sessions-per-user:5}") int maxSessionsPerUser,
            @Value("${app.jwt.persist-plaintext:false}") boolean persistPlaintext) {
        this.repository = repository;
        this.refreshDays = refreshDays;
        this.maxSessionsPerUser = maxSessionsPerUser;
        this.persistPlaintext = persistPlaintext;
    }

    /** DTO ligero para evitar LazyInitialization fuera del servicio */
    public record UserRef(long id, String username) {}

    /** Se devuelve el plaintext SOLO en la respuesta HTTP (no en DB) */
    public record IssuedRefresh(String plain, long expiresInSeconds) {}

    /** Resultado de rotación: usuario + nuevo refresh (plaintext + ttl) */
    public record RotationResult(UserRef user, IssuedRefresh newRefresh) {}

    /** Crea refresh y aplica cap por usuario. */
    public IssuedRefresh create(com.oscar.proyecto.ms_auth.user.User user) {
        String plain = generateOpaqueToken();
        String hash = sha256Url(plain);

        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        if (persistPlaintext) { // compat opcional
            rt.setToken(plain);
        }
        rt.setTokenHash(hash);    // validación real
        rt.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshDays)));
        repository.save(rt);

        enforceUserSessionCap(user.getId());

        return new IssuedRefresh(plain, getRefreshExpirationSeconds());
    }

    /** Validación segura por HASH + fetch join (para /auth/refresh, /logout-all). */
    public UserRef validateAndGetUserRef(String tokenPlain) {
        String hash = sha256Url(tokenPlain);
        RefreshToken rt = repository.findByTokenHashFetchUser(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (rt.isRevoked() || rt.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired or revoked refresh token");
        }
        var u = rt.getUser();
        return new UserRef(u.getId(), u.getUsername());
    }

    /** Rotar: revoca el usado (por HASH) y emite uno nuevo. */
    public RotationResult rotate(String usedRefreshPlain) {
        String usedHash = sha256Url(usedRefreshPlain);
        RefreshToken current = repository.findByTokenHashFetchUser(usedHash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (current.isRevoked() || current.getExpiresAt().isBefore(Instant.now())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Expired or revoked refresh token");
        }

        current.setRevoked(true);
        repository.save(current);

        String nextPlain = generateOpaqueToken();
        String nextHash = sha256Url(nextPlain);

        RefreshToken next = new RefreshToken();
        next.setUser(current.getUser());
        if (persistPlaintext) { // compat opcional
            next.setToken(nextPlain);
        }
        next.setTokenHash(nextHash);   // validación real
        next.setExpiresAt(Instant.now().plus(Duration.ofDays(refreshDays)));
        repository.save(next);

        enforceUserSessionCap(current.getUser().getId());

        return new RotationResult(new UserRef(current.getUser().getId(), current.getUser().getUsername()),
                new IssuedRefresh(nextPlain, getRefreshExpirationSeconds()));
    }

    /** Revoca un refresh recibido desde cliente (por HASH). */
    public void revoke(String tokenPlain) {
        String hash = sha256Url(tokenPlain);
        repository.findByTokenHash(hash).ifPresent(repository::delete);
    }

    /** Cierra sesión en todos los dispositivos del usuario. */
    public void revokeAllByUserId(long userId) {
        List<RefreshToken> tokens = repository.findAllByUserId(userId);
        if (!tokens.isEmpty()) {
            tokens.forEach(rt -> rt.setRevoked(true));
            repository.saveAll(tokens);
        }
    }

    public long getRefreshExpirationSeconds() {
        return Duration.ofDays(refreshDays).toSeconds();
    }

    private String generateOpaqueToken() {
        byte[] bytes = new byte[64]; // 512 bits
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Utilidad compartida: SHA-256 y Base64 URL-safe sin padding (~43-44 chars). */
    static String sha256Url(String input) { // visibilidad de paquete para reuso en backfill
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(input.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(digest);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot compute SHA-256", e);
        }
    }

    /** Elimina los refresh tokens más antiguos cuando el usuario supera el máximo permitido. */
    private void enforceUserSessionCap(long userId) {
        if (maxSessionsPerUser <= 0) return;

        long count = repository.countByUserId(userId);
        if (count <= maxSessionsPerUser) return;

        int extras = (int) (count - maxSessionsPerUser);
        List<Long> oldestIds = repository.findIdsByUserOldestFirst(userId);
        if (oldestIds.size() >= extras) {
            repository.deleteAllByIdInBatch(oldestIds.subList(0, extras));
        }
    }
}
