package com.oscar.proyecto.ms_auth.api.dto;

public record LoginResponse(String accessToken, long expiresInSeconds) {
    public String getTokenType() {
        return "Bearer";
    }
}
