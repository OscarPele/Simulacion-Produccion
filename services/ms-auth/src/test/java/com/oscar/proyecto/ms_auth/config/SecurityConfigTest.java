package com.oscar.proyecto.ms_auth.config;

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

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Verifica:
 *  - 401 cuando no hay token en rutas protegidas
 *  - 200 con token válido (y el principal correcto)
 *  - 401 con token inválido
 *  - 200 en una ruta permitida (actuator/health)
 *
 * Usa H2 en memoria para evitar dependencia de Postgres.
 */
@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
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
}
