package com.oscar.proyecto.ms_auth.jwt;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.time.Instant;
import java.util.Base64;
import java.util.Date;
import java.util.Map;
import java.util.UUID;

@Service
public class JwtService {

    private final String issuer;
    private final String audience;

    private final PrivateKey privateKey;
    private final PublicKey publicKey;
    private final long expirationSeconds;

    public JwtService(
            @Value("${app.jwt.private-key-location:classpath:jwt/private.pem}") Resource privateKeyLocation,
            @Value("${app.jwt.public-key-location:classpath:jwt/public.pem}") Resource publicKeyLocation,
            @Value("${app.jwt.expiration-minutes}") long expirationMinutes,
            @Value("${app.jwt.issuer:ms-auth}") String issuer,
            @Value("${app.jwt.audience:api}") String audience
    ) {
        this.privateKey = loadPrivateKey(privateKeyLocation);
        this.publicKey  = loadPublicKey(publicKeyLocation);
        this.expirationSeconds = expirationMinutes * 60;
        this.issuer = issuer;
        this.audience = audience;
    }

    public String generate(String subject, Map<String, Object> claims) {
        Instant now = Instant.now();
        return Jwts.builder()
                .setHeaderParam("typ", "JWT")
                .setSubject(subject)                                     // sub
                .setIssuer(issuer)                                       // iss
                .setAudience(audience)                                   // aud
                .setId(UUID.randomUUID().toString())                     // jti
                .setIssuedAt(Date.from(now))                             // iat
                .setExpiration(Date.from(now.plusSeconds(expirationSeconds))) // exp
                .addClaims(claims)                                       // uid, roles...
                .signWith(privateKey, SignatureAlgorithm.RS256)
                .compact();
    }

    /** Disponible para tests y validación local. */
    public Claims parseClaims(String token) {
        try {
            return Jwts.parserBuilder()
                    .setSigningKey(publicKey)
                    .requireIssuer(issuer)
                    .requireAudience(audience)
                    .setAllowedClockSkewSeconds(60)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
        } catch (JwtException | IllegalArgumentException e) {
            return null;
        }
    }

    public long getExpirationSeconds() {
        return expirationSeconds;
    }

    // ===== helpers =====
    private static PrivateKey loadPrivateKey(Resource location) {
        try (var in = location.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem.replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePrivate(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load RSA private key from " + safeDesc(location), e);
        }
    }

    private static PublicKey loadPublicKey(Resource location) {
        try (var in = location.getInputStream()) {
            String pem = new String(in.readAllBytes(), StandardCharsets.UTF_8);
            String base64 = pem.replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(base64);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(der);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        } catch (Exception e) {
            throw new IllegalStateException("Cannot load RSA public key from " + safeDesc(location), e);
        }
    }

    private static String safeDesc(Resource r) {
        try { return r.getDescription(); } catch (Exception ignored) { return "<resource>"; }
    }
}
