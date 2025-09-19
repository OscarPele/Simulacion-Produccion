package com.oscar.proyecto.ms_auth.verification;

import com.oscar.proyecto.ms_auth.mail.MailSenderPort;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;

@Service
public class EmailVerificationService {

    private final EmailVerificationTokenRepository tokens;
    private final UserRepository users;
    private final MailSenderPort mail;
    private final SecureRandom rnd = new SecureRandom();

    private final Duration ttl;
    private final String backendVerifyUrl;
    private final String frontendSuccessUrl;
    private final String frontendErrorUrl;

    public EmailVerificationService(
            EmailVerificationTokenRepository tokens,
            UserRepository users,
            MailSenderPort mail,
            @Value("${app.verify-email.ttl-hours:24}") long ttlHours,
            @Value("${app.verify-email.backend-verify-url:}") String backendVerifyUrl,
            @Value("${app.verify-email.frontend-success-url:https://opsimulator.com/verified}") String frontendSuccessUrl,
            @Value("${app.verify-email.frontend-error-url:https://opsimulator.com/verify-error}") String frontendErrorUrl
    ) {
        this.tokens = tokens;
        this.users = users;
        this.mail = mail;
        this.ttl = Duration.ofHours(ttlHours);
        this.backendVerifyUrl = backendVerifyUrl == null ? "" : backendVerifyUrl.trim();
        this.frontendSuccessUrl = frontendSuccessUrl;
        this.frontendErrorUrl = frontendErrorUrl;
    }

    /** Enviar (o reenviar) verificación al usuario (idempotente: invalida previos no usados). */
    @Transactional
    public void send(User u) {
        // invalida tokens anteriores no usados
        tokens.deleteByUser_IdAndUsedAtIsNull(u.getId());

        // genera token (plaintext) y guarda solo su hash
        String plain = randomToken();
        String hash = sha256(plain);

        var t = new EmailVerificationToken();
        t.setUser(u);
        t.setTokenHash(hash);
        t.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(t);

        // link de verificación: preferimos backend si está configurado
        String link = buildVerifyLink(plain);

        // Email HTML mínimo (lo estilizarás más adelante)
        String html = """
            <div style="font-family:system-ui,-apple-system,Segoe UI,Roboto,Helvetica,Arial,sans-serif;line-height:1.5">
              <h2>Confirma tu correo</h2>
              <p>Para activar tu cuenta, haz clic aquí:</p>
              <p><a href="%s" style="display:inline-block;padding:10px 16px;background:#2563eb;color:#fff;text-decoration:none;border-radius:6px">Verificar correo</a></p>
              <p>Si no funciona, copia y pega el enlace en tu navegador:<br><code>%s</code></p>
              <p>Caduca en %d horas.</p>
            </div>
        """.formatted(link, link, ttl.toHours());

        mail.send(u.getEmail(), "Verifica tu correo", html);
    }

    /**
     * Confirma un token (versión "API"): levanta 400 en token inválido/expirado/ya usado.
     * Útil si quieres seguir teniendo un endpoint que responda 204/400 sin redirigir.
     */
    @Transactional
    public void confirm(String plainToken) {
        String hash = sha256(plainToken);
        var t = tokens.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST));

        if (t.isUsed() || t.isExpired()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST);

        t.setUsedAt(Instant.now());
        tokens.save(t);

        var u = t.getUser();
        u.setEnabled(true);
        users.save(u);

        // Limpieza: borra otros tokens no usados del mismo usuario
        tokens.deleteByUser_IdAndUsedAtIsNull(u.getId());
    }

    /**
     * Confirma y devuelve la URL a la que debe redirigirse el navegador.
     * Éxito -> frontendSuccessUrl
     * Error  -> frontendErrorUrl?reason=CODE
     */
    @Transactional
    public String confirmAndGetRedirectUrl(String plainToken) {
        String hash = sha256(plainToken);
        var opt = tokens.findByTokenHash(hash);

        if (opt.isEmpty()) {
            return frontendErrorUrl + "?reason=INVALID_TOKEN";
        }

        var t = opt.get();

        if (t.isExpired()) {
            tokens.delete(t); // limpieza de caducado
            return frontendErrorUrl + "?reason=TOKEN_EXPIRED";
        }

        if (t.isUsed()) {
            return frontendErrorUrl + "?reason=TOKEN_ALREADY_USED";
        }

        // marcar usado y habilitar usuario
        t.setUsedAt(Instant.now());
        tokens.save(t);

        var u = t.getUser();
        u.setEnabled(true);
        users.save(u);

        // Limpieza: borra otros tokens no usados del mismo usuario
        tokens.deleteByUser_IdAndUsedAtIsNull(u.getId());

        return frontendSuccessUrl;
    }

    // ===== helpers =====

    private String randomToken() {
        byte[] b = new byte[32]; // 256 bits
        rnd.nextBytes(b);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(b);
    }

    private String sha256(String s) {
        try {
            var md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(s.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(d);
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private String buildVerifyLink(String plainToken) {
        if (!backendVerifyUrl.isBlank()) {
            return backendVerifyUrl + "?token=" + plainToken;
        }
        // fallback: si no hay backend-verify-url, asumimos front renderiza y luego llama a la API
        // por ejemplo: https://opsimulator.com/verify?token=...
        String base = frontendSuccessUrl.replace("/verified", "/verify");
        return base + "?token=" + plainToken;
    }

    public String getFrontendSuccessUrl() { return frontendSuccessUrl; }
    public String getFrontendErrorUrl() { return frontendErrorUrl; }
}
