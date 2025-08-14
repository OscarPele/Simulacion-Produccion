package com.oscar.proyecto.ms_auth.password;

import com.oscar.proyecto.ms_auth.mail.MailSenderPort;
import com.oscar.proyecto.ms_auth.token.RefreshTokenService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Service
public class PasswordResetService {

    private final UserService userService;
    private final PasswordResetTokenRepository tokens;
    private final MailSenderPort mailer;
    private final RefreshTokenService refreshTokens;

    // TTL del token de reset
    private final Duration ttl;

    // URL base del frontend para el enlace del email
    private final String frontendBaseUrl;

    public PasswordResetService(UserService userService,
                                PasswordResetTokenRepository tokens,
                                MailSenderPort mailer,
                                RefreshTokenService refreshTokens,
                                @Value("${app.password-reset.ttl-minutes:15}") long ttlMinutes,
                                @Value("${app.frontend-url:http://localhost:5173}") String frontendBaseUrl) {
        this.userService = userService;
        this.tokens = tokens;
        this.mailer = mailer;
        this.refreshTokens = refreshTokens;
        this.ttl = Duration.ofMinutes(ttlMinutes);
        this.frontendBaseUrl = frontendBaseUrl;
    }

    /**
     * No revela si el email existe: siempre completa sin error.
     */
    @Transactional
    public void requestReset(String email) {
        Optional<User> opt = userService.findByEmailIgnoreCase(email);
        if (opt.isEmpty()) return;

        User user = opt.get();

        // Generar token opaco en claro (para el link) y almacenar SOLO el hash
        String raw = TokenUtils.newBase64UrlToken();
        String hash = TokenUtils.sha256Base64Url(raw);

        PasswordResetToken prt = new PasswordResetToken();
        prt.setUserId(user.getId());
        prt.setTokenHash(hash);
        prt.setCreatedAt(Instant.now());
        prt.setExpiresAt(Instant.now().plus(ttl));
        tokens.save(prt);

        String url = frontendBaseUrl.replaceAll("/$", "") + "/reset-password?token=" + raw;

        String subject = "Restablece tu contraseña";
        String body = """
                <p>Hola %s,</p>
                <p>Has solicitado restablecer tu contraseña. Este enlace caduca en %d minutos:</p>
                <p><a href="%s">%s</a></p>
                <p>Si no fuiste tú, ignora este mensaje.</p>
                """.formatted(user.getUsername(), ttl.toMinutes(), url, url);

        mailer.send(user.getEmail(), subject, body);
    }

    /**
     * Valida token (existencia, no usado, no expirado), cambia contraseña y revoca sesiones.
     */
    @Transactional
    public void reset(String rawToken, String newPassword) {
        String hash = TokenUtils.sha256Base64Url(rawToken);
        PasswordResetToken prt = tokens.findByTokenHash(hash)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOKEN_INVALID"));

        if (prt.getUsedAt() != null)
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOKEN_USED");

        if (Instant.now().isAfter(prt.getExpiresAt()))
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "TOKEN_EXPIRED");

        // Cambiar contraseña del usuario
        userService.forceChangePassword(prt.getUserId(), newPassword);

        // Marcar token como usado
        prt.setUsedAt(Instant.now());
        tokens.save(prt);

        // Revocar todas las sesiones del usuario
        refreshTokens.revokeAllByUserId(prt.getUserId());
    }
}
