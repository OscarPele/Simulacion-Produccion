package com.oscar.proyecto.ms_auth.api.dto;

import java.time.Instant;

public class UserResponse {
    private final Long id;
    private final String username;
    private final String email;
    private final boolean enabled;
    private final Instant createdAt;

    public UserResponse(Long id, String username, String email, boolean enabled, Instant createdAt) {
        this.id = id;
        this.username = username;
        this.email = email;
        this.enabled = enabled;
        this.createdAt = createdAt;
    }

    public Long getId() { return id; }
    public String getUsername() { return username; }
    public String getEmail() { return email; }
    public boolean isEnabled() { return enabled; }
    public Instant getCreatedAt() { return createdAt; }
}
