package com.oscar.proyecto.ms_auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceExpiryTest {
    // expira “ya”
    private final JwtService jwt = new JwtService(
            "not-base64:super-secret-key-0123456789-0123456789-0123456789",
            0
    );

    @Test
    void expired_token_returns_null() {
        String token = jwt.generate("alice", java.util.Map.of());
        Claims claims = jwt.parseClaims(token);
        assertNull(claims); // exp(ahora) → se considera expirado
    }
}
