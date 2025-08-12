package com.oscar.proyecto.ms_auth.api;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.oscar.proyecto.ms_auth.api.dto.LoginRequest;
import com.oscar.proyecto.ms_auth.api.dto.RegisterRequest;
import com.oscar.proyecto.ms_auth.exception.GlobalExceptionHandler;
import com.oscar.proyecto.ms_auth.exception.InvalidCredentialsException;
import com.oscar.proyecto.ms_auth.jwt.JwtService;
import com.oscar.proyecto.ms_auth.user.User;
import com.oscar.proyecto.ms_auth.user.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;

import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.time.Instant;

import static org.mockito.ArgumentMatchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper om;

    @MockitoBean
    UserService userService;

    @MockitoBean
    JwtService jwtService;

    @Test
    @DisplayName("POST /auth/login → 200 con token")
    void login_ok() throws Exception {
        var user = new User();
        user.setId(1L);
        user.setUsername("alice");
        user.setEmail("alice@mail.com");

        Mockito.when(userService.authenticate("alice", "Secret123")).thenReturn(user);
        Mockito.when(jwtService.generate(eq("alice"), anyMap())).thenReturn("jwt.token.value");
        Mockito.when(jwtService.getExpirationSeconds()).thenReturn(900L);

        var req = new LoginRequest();
        req.setUsernameOrEmail("alice");
        req.setPassword("Secret123");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").value("jwt.token.value"))
                .andExpect(jsonPath("$.expiresInSeconds").value(900))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    @DisplayName("POST /auth/login credenciales inválidas → 401")
    void login_invalid_returns_401() throws Exception {
        Mockito.when(userService.authenticate(anyString(), anyString()))
                .thenThrow(new InvalidCredentialsException());

        var req = new LoginRequest();
        req.setUsernameOrEmail("alice");
        req.setPassword("bad");

        mvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
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

        var req = new RegisterRequest();
        req.setUsername("bob");
        req.setEmail("bob@mail.com");
        req.setPassword("Secret123");

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
        var req = new RegisterRequest();
        req.setUsername("");
        req.setEmail("");
        req.setPassword("123");

        mvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(om.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }
}
