package com.oscar.proyecto.ms_auth.api.dto;

import java.time.Instant;

public record UserResponse(
        Long id,
        String username,
        String email,
        boolean enabled,
        Instant createdAt
) {}
