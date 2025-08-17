package com.oscar.shared.security;

import jakarta.servlet.http.HttpServletResponse; 
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;

@Configuration
@EnableWebSecurity
public class MSSecurityConfig {

    private final ResourceLoader resourceLoader;

    public MSSecurityConfig(ResourceLoader resourceLoader) {
        this.resourceLoader = resourceLoader;
    }

    @Value("${app.jwt.issuer}")
    private String issuer;

    @Value("${app.jwt.public-key-location:classpath:jwt/public.pem}")
    private String publicKeyLocation;

    @Value("${security.permit-all:/actuator/health,/actuator/info}")
    private String[] permitAll;

    // --- Beans overridables (cada MS puede proporcionar los suyos si quiere) ---

    @Bean
    @ConditionalOnMissingBean(PublicKey.class)
    public PublicKey jwtPublicKey() throws Exception {
        Resource res = resourceLoader.getResource(publicKeyLocation);
        try (InputStream is = res.getInputStream()) {
            String pem = new String(is.readAllBytes());
            String clean = pem
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replaceAll("\\s", "");
            byte[] der = Base64.getDecoder().decode(clean);
            return KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(der));
        }
    }

    @Bean
    @ConditionalOnMissingBean(MSJwtAuthFilter.class)
    public MSJwtAuthFilter msJwtAuthFilter(PublicKey jwtPublicKey) {
        return new MSJwtAuthFilter(jwtPublicKey, issuer);
    }

    @Bean
    @ConditionalOnMissingBean(CorsConfigurationSource.class)
    public CorsConfigurationSource corsConfigurationSource(
            @Value("${cors.allowed-origins:}") String allowedOriginsProp,
            @Value("${cors.allowed-methods:GET,POST,PUT,DELETE,OPTIONS,PATCH}") String allowedMethodsProp,
            @Value("${cors.allowed-headers:*}") String allowedHeadersProp
    ) {
        CorsConfiguration cfg = new CorsConfiguration();

        if (allowedOriginsProp != null && !allowedOriginsProp.isBlank()) {
            cfg.setAllowedOrigins(Arrays.stream(allowedOriginsProp.split(",")).map(String::trim).toList());
        } else {
            cfg.setAllowedOriginPatterns(List.of(
                    "http://localhost:*",
                    "http://127.0.0.1:*",
                    "http://192.168.*.*:*",
                    "https://opsimulator.com",
                    "https://www.opsimulator.com"
            ));
        }

        cfg.setAllowedMethods(Arrays.stream(allowedMethodsProp.split(",")).map(String::trim).toList());
        cfg.setAllowedHeaders(Arrays.stream(allowedHeadersProp.split(",")).map(String::trim).toList());
        cfg.setExposedHeaders(List.of("Authorization", "Location"));
        cfg.setAllowCredentials(false);
        cfg.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", cfg);
        return source;
    }

    // --- Cadena de seguridad común ---
    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http, MSJwtAuthFilter jwtFilter) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .cors(Customizer.withDefaults())
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(permitAll).permitAll()
                .requestMatchers(org.springframework.http.HttpMethod.OPTIONS, "/**").permitAll()
                .anyRequest().authenticated()
            )
            .exceptionHandling(ex -> ex
                // sin token / token inválido -> 401
                .authenticationEntryPoint((req, res, e) -> res.sendError(HttpServletResponse.SC_UNAUTHORIZED))
                // con token pero sin permisos -> 403
                .accessDeniedHandler((req, res, e) -> res.sendError(HttpServletResponse.SC_FORBIDDEN))
            )
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter.class)
            .httpBasic(AbstractHttpConfigurer::disable);
        return http.build();
    }
}
