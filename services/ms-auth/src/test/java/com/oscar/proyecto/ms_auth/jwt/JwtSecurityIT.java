package com.oscar.proyecto.ms_auth.jwt;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.annotation.Nullable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Map;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(properties = {
        // DB en memoria para que arranque el contexto
        "spring.datasource.url=jdbc:h2:mem:authdb;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "spring.datasource.driverClassName=org.h2.Driver",
        "spring.datasource.username=sa",
        "spring.datasource.password=",
        "spring.jpa.hibernate.ddl-auto=create-drop",
        // log mínimo para no ensuciar salida
        "logging.level.org.springframework.security=ERROR",
        // (opcional) asegura issuer si cambiases yml en el futuro
        "app.jwt.issuer=ms-auth",
        "app.jwt.public-key-location=classpath:jwt/public.pem",
        "spring.autoconfigure.exclude=org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration"

})
@AutoConfigureMockMvc
@Import(JwtSecurityIT.ProtectedController.class)
class JwtSecurityIT {

    @Autowired MockMvc mvc;
    @Autowired JwtService jwtService;

    @RestController
    static class ProtectedController {
        @GetMapping("/protected/hello")
        public Map<String,Object> hello(@Nullable Authentication auth) {
            return Map.of("user", auth != null ? auth.getName() : "anonymous");
        }
    }

    @Test
    void valid_bearer_token_allows_access() throws Exception {
        String token = jwtService.generate("alice", Map.of("uid", 1L));
        mvc.perform(get("/protected/hello").header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user").value("alice"));
    }

    @Test
    void missing_or_invalid_token_is_unauthorized() throws Exception {
        mvc.perform(get("/protected/hello")).andExpect(status().isUnauthorized());
        mvc.perform(get("/protected/hello").header("Authorization", "Bearer not.a.real.token"))
                .andExpect(status().isUnauthorized());
    }
}


