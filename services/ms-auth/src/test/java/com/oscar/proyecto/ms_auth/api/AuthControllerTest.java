package com.oscar.proyecto.ms_auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.proyecto.ms_auth.api.dto.LoginRequest;
import com.oscar.proyecto.ms_auth.api.dto.RegisterRequest;
import com.oscar.proyecto.ms_auth.api.dto.RefreshTokenRequest;
import com.oscar.proyecto.ms_auth.exception.GlobalExceptionHandler;
import com.oscar.proyecto.ms_auth.exception.InvalidCredentialsException;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.token.RefreshToken;
import com.oscar.proyecto.ms_auth.token.RefreshTokenService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.Map;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper om;

    @MockitoBean UserService userService;
    @MockitoBean JwtService jwtService;
    @MockitoBean RefreshTokenService refreshTokenService;

    @Test
    @DisplayName("POST /auth/login → 200 con token")
    void login_ok() throws Exception {
        var user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@mail.com");

        var rt = new RefreshToken();
        rt.setToken("opaque-refresh");

        Mockito.when(userService.authenticate("alice", "Secret123")).thenReturn(user);
        Mockito.when(jwtService.generate(eq("alice"), anyMap())).thenReturn("jwt.token.value");
        Mockito.when(jwtService.getExpirationSeconds()).thenReturn(900L);
        Mockito.when(refreshTokenService.create(user)).thenReturn(rt);
        Mockito.when(refreshTokenService.getRefreshExpirationSeconds()).thenReturn(604800L);

        var req = new LoginRequest("alice", "Secret123");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("jwt.token.value"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("opaque-refresh"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800));
    }

    @Test
    @DisplayName("POST /auth/login credenciales inválidas → 401")
    void login_invalid_returns_401() throws Exception {
        Mockito.when(userService.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        var req = new LoginRequest("alice", "bad");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/login con campos en blanco → 400")
    void login_blank_fields_returns_400() throws Exception {
        var req = new LoginRequest("", "");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/register → 200 con datos del usuario")
    void register_ok() throws Exception {
        var created = new User();
        created.setId(5L);
        created.setUsername("bob");
        created.setEmail("bob@mail.com");
        created.setEnabled(true);
        created.setCreatedAt(Instant.parse("2024-01-01T00:00:00Z"));

        Mockito.when(userService.register("bob", "bob@mail.com", "Secret123"))
                .thenReturn(created);

        var req = new RegisterRequest("bob", "bob@mail.com", "Secret123");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(5))
                .andExpect(jsonPath("$.username").value("bob"))
                .andExpect(jsonPath("$.email").value("bob@mail.com"))
                .andExpect(jsonPath("$.enabled").value(true));
    }

    @Test
    @DisplayName("POST /auth/register inválido (faltan campos) → 400")
    void register_validation_error_400() throws Exception {
        var req = new RegisterRequest("", "", "123");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /auth/refresh → 200 con nuevo access y refresh rotado")
    void refresh_ok() throws Exception {
        var userRef = new RefreshTokenService.UserRef(1L, "alice");
        var newRt = new RefreshToken();
        newRt.setToken("new-opaque-refresh");
        var rotation = new RefreshTokenService.RotationResult(userRef, newRt);

        Mockito.when(refreshTokenService.rotate("old-refresh")).thenReturn(rotation);
        Mockito.when(jwtService.generate("alice", Map.of("uid", 1L))).thenReturn("new.jwt.token");
        Mockito.when(jwtService.getExpirationSeconds()).thenReturn(900L);
        Mockito.when(refreshTokenService.getRefreshExpirationSeconds()).thenReturn(604800L);

        var req = new RefreshTokenRequest("old-refresh");

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.accessToken").value("new.jwt.token"))
                .andExpect(jsonPath("$.expiresIn").value(900))
                .andExpect(jsonPath("$.refreshToken").value("new-opaque-refresh"))
                .andExpect(jsonPath("$.refreshExpiresIn").value(604800));
    }

    @Test
    @DisplayName("POST /auth/refresh con token inválido/expirado → 401")
    void refresh_invalid_returns_401() throws Exception {
        Mockito.when(refreshTokenService.rotate("bad-refresh"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        var req = new RefreshTokenRequest("bad-refresh");

        mvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("POST /auth/logout con refresh inexistente → 204 (idempotente)")
    void logout_with_unknown_refresh_is_204() throws Exception {
        var body = new RefreshTokenRequest("does-not-exist");

        mvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNoContent());

        Mockito.verify(refreshTokenService).revoke("does-not-exist");
    }

    @Test
    @DisplayName("POST /auth/logout-all con refresh inválido → 204 (no se filtra info)")
    void logout_all_with_invalid_refresh_is_204() throws Exception {
        Mockito.when(refreshTokenService.validateAndGetUserRef("bad-rt"))
                .thenThrow(new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid"));

        var body = new RefreshTokenRequest("bad-rt");

        mvc.perform(post("/auth/logout-all")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(body)))
                .andExpect(status().isNoContent());
    }
}
