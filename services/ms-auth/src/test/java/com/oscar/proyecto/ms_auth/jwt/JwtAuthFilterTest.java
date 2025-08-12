package com.oscar.proyecto.ms_auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.impl.DefaultClaims;
import jakarta.servlet.FilterChain;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class JwtAuthFilterTest {

    @AfterEach
    void cleanup() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void sets_authentication_when_token_valid() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwt);

        Claims claims = new DefaultClaims();
        claims.setSubject("alice");
        when(jwt.parseClaims("good.token")).thenReturn(claims);

        MockHttpServletRequest req = new MockHttpServletRequest();
        req.addHeader("Authorization", "Bearer good.token");
        MockHttpServletResponse res = new MockHttpServletResponse();
        FilterChain chain = mock(FilterChain.class);

        filter.doFilter(req, res, chain);

        assertNotNull(SecurityContextHolder.getContext().getAuthentication());
        assertEquals("alice", SecurityContextHolder.getContext().getAuthentication().getName());
        verify(chain).doFilter(req, res);
    }

    @Test
    void does_nothing_when_token_invalid_or_missing() throws Exception {
        JwtService jwt = mock(JwtService.class);
        JwtAuthFilter filter = new JwtAuthFilter(jwt);

        // Caso 1: sin header
        MockHttpServletRequest req1 = new MockHttpServletRequest();
        MockHttpServletResponse res1 = new MockHttpServletResponse();
        FilterChain chain1 = mock(FilterChain.class);
        filter.doFilter(req1, res1, chain1);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain1).doFilter(req1, res1);

        // Caso 2: inválido
        MockHttpServletRequest req2 = new MockHttpServletRequest();
        req2.addHeader("Authorization", "Bearer bad.token");
        MockHttpServletResponse res2 = new MockHttpServletResponse();
        FilterChain chain2 = mock(FilterChain.class);
        when(jwt.parseClaims("bad.token")).thenReturn(null);

        filter.doFilter(req2, res2, chain2);
        assertNull(SecurityContextHolder.getContext().getAuthentication());
        verify(chain2).doFilter(req2, res2);
    }
}
