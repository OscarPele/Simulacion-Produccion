package com.oscar.proyecto.ms_auth.api.dto;

public record LoginResponse(String token, long expiresInSeconds, String tokenType) {
    public LoginResponse(String token, long expiresInSeconds) { this(token, expiresInSeconds, "Bearer"); }
}

