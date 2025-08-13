package com.oscar.shared.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.SignatureException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.lang.Nullable;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.security.PublicKey;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MSJwtAuthFilter extends OncePerRequestFilter {

    private final PublicKey publicKey;
    private final String issuer;
    private final String headerName;
    private final String prefix;

    private static final Logger log = LoggerFactory.getLogger(MSJwtAuthFilter.class);

    public MSJwtAuthFilter(PublicKey publicKey,
                           String issuer) {
        this(publicKey, issuer, "Authorization", "Bearer ");
    }

    public MSJwtAuthFilter(PublicKey publicKey,
                           String issuer,
                           String headerName,
                           String prefix) {
        this.publicKey = Objects.requireNonNull(publicKey);
        this.issuer = Objects.requireNonNull(issuer);
        this.headerName = headerName;
        this.prefix = prefix;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {

        final String uri = req.getRequestURI();
        final String auth = req.getHeader(headerName);
        final boolean hasBearer = (auth != null && auth.startsWith(prefix));

        log.info("JWT filter ENTER uri={} hasBearer={}", uri, hasBearer);

        if (!StringUtils.hasText(auth) || !auth.startsWith(prefix)) {
            log.info("JWT filter PASS (no token) uri={}", uri);
            chain.doFilter(req, res);
            return;
        }

        final String token = auth.substring(prefix.length());

        try {
            Claims claims = Jwts.parserBuilder()
                    .requireIssuer(issuer)
                    .setSigningKey(publicKey)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String username = claims.getSubject();
            if (!StringUtils.hasText(username)) {
                log.warn("JWT filter DENY uri={} code=INVALID_TOKEN_SUBJECT", uri);
                unauthorized(res, "INVALID_TOKEN_SUBJECT");
                return;
            }

            var authorities = mapRoles(claims.get("roles", List.class));
            SecurityContextHolder.getContext().setAuthentication(
                    new UsernamePasswordAuthenticationToken(username, null, authorities)
            );

            log.info("JWT filter OK uri={} sub={} roles={}", uri, username, authorities);
            chain.doFilter(req, res);

        } catch (ExpiredJwtException e) {
            log.warn("JWT filter DENY uri={} code=TOKEN_EXPIRED", uri, e);
            unauthorized(res, "TOKEN_EXPIRED");
        } catch (SignatureException e) { 
            
            log.warn("JWT filter DENY uri={} code=INVALID_SIGNATURE", uri, e);
            unauthorized(res, "INVALID_SIGNATURE");
        } catch (Exception e) {
            log.warn("JWT filter DENY uri={} code=INVALID_TOKEN", uri, e);
            unauthorized(res, "INVALID_TOKEN");
        }
    }

    
    @SuppressWarnings("unchecked")
    private Collection<SimpleGrantedAuthority> mapRoles(@Nullable List<?> rolesRaw) {
        if (rolesRaw == null || rolesRaw.isEmpty()) return List.of();
        return rolesRaw.stream()
                .filter(Objects::nonNull)
                .map(Object::toString)
                .filter(StringUtils::hasText)
                .map(r -> r.startsWith("ROLE_") ? r : "ROLE_" + r)
                .map(SimpleGrantedAuthority::new)
                .collect(Collectors.toList());
    }

    private void unauthorized(HttpServletResponse res, String code) throws IOException {
        res.setStatus(HttpStatus.UNAUTHORIZED.value());
        res.setContentType("application/json");
        res.getWriter().write("{\"code\":\"" + code + "\"}");
        res.getWriter().flush();
    }
}
