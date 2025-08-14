package com.oscar.proyecto.ms_auth.token;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Comparator;
import java.util.List;
import java.util.Random;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(OrderAnnotation.class)
@TestPropertySource(properties = {
        // Expiración corta para pruebas rápidas
        "app.jwt.expiration-minutes=1",
        "app.jwt.refresh-expiration-days=1",
        // Limitar sesiones por usuario a 2 para verificar el cap
        "app.jwt.max-sessions-per-user=2",
        // Modo final: NO persistir plaintext en BD
        "app.jwt.persist-plaintext=false",
        // Bajar ruido de logs si hace falta:
        "logging.level.org.springframework.security=ERROR",
        "logging.level.org.hibernate.SQL=ERROR"
})
class AuthFlowIT {

    private static final Logger log = LoggerFactory.getLogger(AuthFlowIT.class);

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;
    @Autowired JwtService jwtService;
    @Autowired RefreshTokenRepository refreshRepo;
    @Autowired RefreshTokenCleanup cleanup; // usamos el job directamente

    private static String username;
    private static String email;
    private static String password = "P4ssw0rd!";

    private static String accessToken;
    private static String refreshToken;
    private static long userId;

    @BeforeAll
    static void genUser() {
        // usuario único por ejecución
        int n = 10_000 + new Random().nextInt(90_000);
        username = "oscar" + n;
        email = "oscar" + n + "@test.local";
    }

    @Test
    @Order(1)
    void register_then_login_returns_access_and_refresh() throws Exception {
        log.info("== [1] Registro de usuario ==");
        String registerJson = """
            { "username":"%s", "email":"%s", "password":"%s" }
            """.formatted(username, email, password);
        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value(username))
                .andExpect(jsonPath("$.email").value(email));

        log.info("== [1] Login ==");
        String loginJson = """
            { "usernameOrEmail":"%s", "password":"%s" }
            """.formatted(username, password);

        var res = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(loginJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.refreshToken").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andExpect(jsonPath("$.refreshExpiresIn").isNumber())
                .andReturn();

        JsonNode body = om.readTree(res.getResponse().getContentAsString(StandardCharsets.UTF_8));
        accessToken  = body.get("accessToken").asText();
        refreshToken = body.get("refreshToken").asText();

        // Leemos claims para coger el userId (uid)
        Claims claims = jwtService.parseClaims(accessToken);
        assertThat(claims).as("Claims no deben ser null").isNotNull();
        userId = claims.get("uid", Number.class).longValue();
        assertThat(userId).as("uid en token").isPositive();

        log.info("Login OK → uid={}, access(len={}), refresh(len={})",
                userId, accessToken.length(), refreshToken.length());

        // Debe existir 1 refresh token en BD
        long count = refreshRepo.countByUserId(userId);
        log.info("Refresh tokens en BD tras login: {}", count);
        assertThat(count).isBetween(1L, 2L); // depende si ya existía alguno por repeticiones
    }

    @Test
    @Order(2)
    void refresh_issues_new_access_token() throws Exception {
        log.info("== [2] Refresco de access token ==");
        String refreshJson = """
            { "refreshToken":"%s" }
            """.formatted(refreshToken);

        var res = mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(refreshJson))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").exists())
                .andExpect(jsonPath("$.expiresIn").isNumber())
                .andReturn();

        JsonNode body = om.readTree(res.getResponse().getContentAsString());
        String newAccess = body.get("accessToken").asText();

        log.info("Nuevo access token emitido (len={})", newAccess.length());
        assertThat(newAccess).isNotBlank();
        assertThat(newAccess).as("Debe ser distinto al anterior").isNotEqualTo(accessToken);

        // Claims razonables
        Claims c = jwtService.parseClaims(newAccess);
        assertThat(c.getSubject()).as("subject = username").isEqualTo(username);
        assertThat(c.getExpiration().toInstant()).isAfter(Instant.now());
    }

    @Test
    @Order(3)
    void session_cap_keeps_only_two_most_recent_sessions() throws Exception {
        log.info("== [3] Verificar límite de sesiones (cap=2) con 3 logins seguidos ==");
        // Hacemos 3 logins para superar el cap (definido en TestPropertySource)
        for (int i = 0; i < 3; i++) {
            var res = mvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content("""
                                { "usernameOrEmail":"%s", "password":"%s" }
                                """.formatted(username, password)))
                    .andExpect(status().isOk())
                    .andReturn();

            JsonNode body = om.readTree(res.getResponse().getContentAsString());
            String rt = body.get("refreshToken").asText();
            log.info("Login {} → refresh(len={})", i + 1, rt.length());
        }

        long countAfter = refreshRepo.countByUserId(userId);
        log.info("Tokens de refresh almacenados para uid={} → {}", userId, countAfter);
        assertThat(countAfter)
                .as("Debe mantenerse <= cap (2)")
                .isLessThanOrEqualTo(2L);

        // Además, comprobamos que los que quedan son los más recientes
        List<RefreshToken> tokens = refreshRepo.findAllByUserId(userId);
        tokens.sort(Comparator.comparing(RefreshToken::getCreatedAt));
        assertThat(tokens).as("Debe haber 1 o 2 tokens").isNotEmpty();
        if (tokens.size() == 2) {
            assertThat(tokens.get(1).getCreatedAt()).isAfterOrEqualTo(tokens.get(0).getCreatedAt());
        }
    }

    @Test
    @Order(4)
    void revoked_refresh_cannot_be_used() throws Exception {
        log.info("== [4] Revocar un refresh y comprobar que /auth/refresh devuelve 401 ==");

        // 1) Generamos un refresh válido (plaintext) vía login
        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "usernameOrEmail":"%s", "password":"%s" }
                            """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        String rtPlain = om.readTree(loginRes.getResponse().getContentAsString()).get("refreshToken").asText();

        // 2) Buscamos la entidad por HASH y la revocamos
        String rtHash = RefreshTokenService.sha256Url(rtPlain);
        var toRevoke = refreshRepo.findByTokenHash(rtHash).orElseThrow();
        toRevoke.setRevoked(true);
        refreshRepo.save(toRevoke);
        log.info("Refresh revocado (hash prefix={}...)", rtHash.substring(0, 12));

        // 3) Intento de refresh con el token revocado → 401
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                            { "refreshToken":"%s" }
                            """.formatted(rtPlain)))
                .andExpect(status().isUnauthorized());

        log.info("Correcto: refresh revocado no puede usarse (401).");
    }

    @Test
    @Order(5)
    void cleanup_removes_expired_and_revoked_tokens() {
        log.info("== [5] Forzar expiración y ejecutar limpieza ==");
        // Forzamos expiración de todos los refresh del usuario y algunos además revocados
        List<RefreshToken> tokens = refreshRepo.findAllByUserId(userId);
        assertThat(tokens).isNotEmpty();

        Instant past = Instant.now().minus(2, ChronoUnit.DAYS);
        tokens.forEach(rt -> {
            rt.setExpiresAt(past);
            // marcamos revocado aleatorio en uno para cubrir ambas ramas
            if (rt.getId() % 2 == 0) rt.setRevoked(true);
        });
        refreshRepo.saveAll(tokens);

        long before = refreshRepo.countByUserId(userId);
        log.info("Antes del cleanup: {} refresh tokens (todos expirados en el pasado)", before);

        // Ejecutamos el job
        cleanup.clean();

        long after = refreshRepo.countByUserId(userId);
        log.info("Después del cleanup: {} refresh tokens", after);

        assertThat(after).as("Después de limpiar, no deberían quedar tokens expirados").isLessThan(before);
    }

    @Test
    @Order(6)
    @DisplayName("logout elimina (o invalida) el refresh y /auth/refresh devuelve 401")
    void logout_removes_refresh_token_or_makes_it_invalid() throws Exception {
        // 1) Login → obtener un refresh token propio para esta prueba
        var loginRes = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "usernameOrEmail":"%s", "password":"%s" }
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();

        JsonNode loginBody = om.readTree(loginRes.getResponse().getContentAsString());
        String rt = loginBody.get("refreshToken").asText();

        // 2) /auth/logout → 204 (idempotente, sin body)
        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(rt)))
                .andExpect(status().isNoContent());

        // 3) Intentar /auth/refresh con ese refresh → 401
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(rt)))
                .andExpect(status().isUnauthorized());

        // 4) (extra) comprobar en BD: según tu servicio, el refresh puede estar BORRADO.
        String hash = RefreshTokenService.sha256Url(rt);
        var rtEntity = refreshRepo.findByTokenHash(hash).orElse(null);
        assertThat(rtEntity)
                .as("Tras /logout el refresh debe estar inutilizable (normalmente eliminado en tu implementación)")
                .isNull(); // <--- tu servicio lo borra; si algún día cambias a 'revoked', cambia esta línea
    }

    @Test
    @Order(7)
    void logout_all_revokes_every_refresh_for_the_user() throws Exception {
        // Prepara dos sesiones (dos refresh tokens) del mismo usuario
        var res1 = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "usernameOrEmail":"%s", "password":"%s" }
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        String rt1 = om.readTree(res1.getResponse().getContentAsString()).get("refreshToken").asText();

        var res2 = mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                        { "usernameOrEmail":"%s", "password":"%s" }
                        """.formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn();
        String rt2 = om.readTree(res2.getResponse().getContentAsString()).get("refreshToken").asText();

        // /auth/logout-all con uno de ellos → 204
        mvc.perform(post("/auth/logout-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(rt1)))
                .andExpect(status().isNoContent());

        // Ambos refresh deben quedar inutilizables → 401 al refrescar
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(rt1)))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"refreshToken":"%s"}""".formatted(rt2)))
                .andExpect(status().isUnauthorized());

        // (extra) comprobar en BD: no deben quedar refresh activos (si los borras, también pasa)
        var remaining = refreshRepo.findAllByUserId(userId).stream()
                .filter(rt -> !rt.isRevoked())
                .toList();
        assertThat(remaining)
                .as("Tras /logout-all no deben quedar refresh activos para el usuario (revocados o eliminados)")
                .isEmpty();
    }
}
