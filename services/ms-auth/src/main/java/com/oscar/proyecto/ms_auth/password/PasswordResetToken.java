package com.oscar.proyecto.ms_auth.password;

import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "password_reset_tokens", indexes = {
        @Index(name = "idx_prt_token_hash", columnList = "tokenHash", unique = true),
        @Index(name = "idx_prt_user_id", columnList = "userId")
})
public class PasswordResetToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false) private Long userId;

    // SHA-256 en base64url (43-44 chars). Guardamos SOLO el hash.
    @Column(nullable = false, length = 64) private String tokenHash;

    @Column(nullable = false) private Instant expiresAt;

    // null si no se ha usado
    @Column private Instant usedAt;

    @Column(nullable = false) private Instant createdAt;

    // getters/setters
    public Long getId() { return id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }
    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }
    public Instant getUsedAt() { return usedAt; }
    public void setUsedAt(Instant usedAt) { this.usedAt = usedAt; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
}
