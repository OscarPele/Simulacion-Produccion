package com.oscar.proyecto.ms_auth.api.dto;

public class LoginResponse {
    private final String accessToken;
    private final long expiresInSeconds;

    public LoginResponse(String accessToken, long expiresInSeconds) {
        this.accessToken = accessToken;
        this.expiresInSeconds = expiresInSeconds;
    }

    public String getAccessToken() { return accessToken; }
    public String getTokenType() {
        return "Bearer"; }
    public long getExpiresInSeconds() { return expiresInSeconds; }
}
