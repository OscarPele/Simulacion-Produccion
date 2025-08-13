package com.oscar.proyecto.ms_auth.api.dto;

public record TokenResponse(
        String tokenType,
        String accessToken,
        long expiresIn,
        String refreshToken,
        long refreshExpiresIn
) {}
