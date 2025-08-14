package com.oscar.proyecto.ms_auth.config;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica:
 *  - 401 cuando no hay token en rutas protegidas
 *  - 200 con token válido (y el principal correcto)
 *  - 401 con token inválido
 *  - 200 en una ruta permitida (actuator/health)
 *  - logout revoca el refresh token y ya no permite /auth/refresh (401)
 *
 * Usa H2 en memoria para evitar dependencia de Postgres.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // evitar inMemoryUser por defecto
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration",
        // ruidos fuera
        "logging.level.org.springframework.security=ERROR",
        // issuer/keys para shared.security / JwtService
        "app.jwt.issuer=ms-auth",
        "app.jwt.public-key-location=classpath:jwt/public.pem"
})
@AutoConfigureMockMvc
@Import(SecurityConfigTest.ProtectedController.class)
class SecurityConfigTest {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;
    @Autowired ObjectMapper om;

    /** Endpoint protegido solo para el contexto de test */
    @RestController
    static class ProtectedController {
        @GetMapping("/protected/ping")
        public Map<String, Object> ping(Authentication auth) {
            return Map.of("user", auth != null ? auth.getName() : "anonymous");
        }
    }

    @Test
    @DisplayName("GET /protected/ping sin token → 401")
    void protected_without_token_is_401() throws Exception {
        mvc.perform(get("/protected/ping"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /protected/ping con token válido → 200 y principal correcto")
    void protected_with_valid_token_is_200() throws Exception {
        String token = jwtService.generate("alice", Map.of("uid", 1L));

        mvc.perform(get("/protected/ping")
                        .header("Authorization", "Bearer " + token)
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.user").value("alice"));
    }

    @Test
    @DisplayName("GET /protected/ping con token inválido → 401")
    void protected_with_invalid_token_is_401() throws Exception {
        mvc.perform(get("/protected/ping")
                        .header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("GET /actuator/health (permitAll) → 200")
    void health_is_permit_all() throws Exception {
        mvc.perform(get("/actuator/health"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("POST /auth/logout revoca el refresh y /auth/refresh devuelve 401")
    void logout_revokes_refresh_token_and_refresh_returns_401() throws Exception {
        // Datos únicos por ejecución
        int n = 10_000 + new Random().nextInt(90_000);
        String username = "revuser" + n;
        String email = "revuser" + n + "@test.local";
        String password = "P4ssw0rd!";

        // 1) Registro
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "username":"%s", "email":"%s", "password":"%s" }
                            """.formatted(username, email, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        // 2) Login → obtener refreshToken en claro
        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "usernameOrEmail":"%s", "password":"%s" }
                            """.formatted(username, password)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andReturn();

        String body = loginRes.getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode json = om.readTree(body);
        String refreshToken = json.get("refreshToken").asText();
        assertThat(refreshToken).isNotBlank();

        // 3) /auth/logout → 204 (revoca el refresh)
        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"refreshToken":"%s"}
                            """.formatted(refreshToken)))
                .andExpect(status().isNoContent());

        // 4) Intento de /auth/refresh con el mismo refresh → 401 (revocado)
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            {"refreshToken":"%s"}
                            """.formatted(refreshToken)))
                .andExpect(status().isUnauthorized());
    }
}
