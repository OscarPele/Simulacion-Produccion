package com.oscar.proyecto.ms_auth.jwt;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private final JwtService jwt = new JwtService(
            "not-base64:super-secret-key-0123456789-0123456789-0123456789",
            15 // minutos
    );

    @Test
    void generate_and_parse_claims_ok() {
        String token = jwt.generate(
                "alice",
                Map.of("uid", 1L, "email", "alice@mail.com")
        );

        assertNotNull(token, "Debe generar un token no nulo");

        Claims claims = jwt.parseClaims(token);
        assertNotNull(claims, "El token válido debe parsearse (no null)");
        assertEquals("alice", claims.getSubject());
        assertEquals(1L, ((Number) claims.get("uid")).longValue());
        assertEquals("alice@mail.com", claims.get("email"));
    }

    @Test
    void tampered_token_is_rejected() {
        String token = jwt.generate("bob", Map.of("uid", 99));

        // Rompemos la firma cambiando el último carácter
        String broken = token.substring(0, token.length() - 1) + "X";

        Claims claims = jwt.parseClaims(broken);
        assertNull(claims, "Un token manipulado debe devolver null en parseClaims()");
    }
}
