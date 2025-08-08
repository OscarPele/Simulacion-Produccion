package com.oscar.proyecto.ms_auth.user;

public record LoginResponse(String token, long expiresIn) {}
