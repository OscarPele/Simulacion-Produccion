package com.oscar.proyecto.ms_auth.token;

import com.oscar.proyecto.ms_auth.user.User;
import jakarta.persistence.*;
import java.time.Instant;

@Entity
@Table(name = "refresh_tokens",
        indexes = {
                // Mantenemos los índices declarados para no romper generación/validación.
                // Si usas migraciones, podrás eliminarlos en el paso final.
                @Index(name = "idx_rt_token", columnList = "token", unique = true),
                @Index(name = "idx_rt_token_hash", columnList = "token_hash", unique = true),
                @Index(name = "idx_rt_user", columnList = "user_id"),
                @Index(name = "idx_rt_expires", columnList = "expiresAt"),
                @Index(name = "idx_rt_revoked", columnList = "revoked")
        })
public class RefreshToken {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, foreignKey = @ForeignKey(name = "fk_rt_user"))
    private User user;

    @Column(name = "token", nullable = true, unique = false, length = 200)
    private String token;

    // Hash base64url(SHA-256(token)) – campo de validación real
    @Column(name = "token_hash", unique = true, length = 64, nullable = false)
    private String tokenHash;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    // getters y setters
    public Long getId() { return id; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public String getToken() { return token; }
    public void setToken(String token) { this.token = token; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public Instant getExpiresAt() { return expiresAt; }
    public void setExpiresAt(Instant expiresAt) { this.expiresAt = expiresAt; }

    public boolean isRevoked() { return revoked; }
    public void setRevoked(boolean revoked) { this.revoked = revoked; }

    public Instant getCreatedAt() { return createdAt; }
}
