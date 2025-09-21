package com.oscar.ms_production;

import com.oscar.shared.security.MSSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Bean;
import org.springframework.core.env.Environment;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;

@SpringBootApplication
@Import(MSSecurityConfig.class)
public class MsProductionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProductionApplication.class, args);
    }

    @Bean
    public org.springframework.boot.CommandLineRunner prodStartupInfo(Environment env) {
        return args -> {
            System.out.println("[ms-production] === STARTUP ===");
            System.out.println("[ms-production] Active profiles: " + Arrays.toString(env.getActiveProfiles()));
            System.out.println("[ms-production] server.port = " + env.getProperty("server.port"));
            System.out.println("[ms-production] app.jwt.public-key-location = " + env.getProperty("app.jwt.public-key-location"));
            System.out.println("[ms-production] =================");
        };
    }

    @Bean
    public OncePerRequestFilter prodProbeFilter() {
        return new OncePerRequestFilter() {
            @Override
            protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
                    throws ServletException, IOException {
                String uri = request.getRequestURI();
                if (uri.startsWith("/api/production/")) {
                    String auth = request.getHeader("Authorization");
                    System.out.println("[ms-production][IN] " + request.getMethod() + " " + uri
                            + " | AuthPresent=" + (auth != null));
                }
                chain.doFilter(request, response);
            }
        };
    }

    @RestController
    static class ProductionProbeController {
        @GetMapping("/api/production/public/ping")
        public Map<String, Object> publicPing(HttpServletRequest req) {
            System.out.println("[ms-production][CTRL] /public/ping");
            return Map.of(
                    "service", "ms-production",
                    "status", "ok",
                    "timestamp", Instant.now().toString()
            );
        }

        @GetMapping("/api/production/secure/me")
        public Map<String, Object> secureMe(Authentication auth, HttpServletRequest req) {
            System.out.println("[ms-production][CTRL] /secure/me");
            if (auth == null) {
                System.out.println("[ms-production][CTRL] Authentication es null -> 401 debería venir del filtro");
                return Map.of("error", "no-auth");
            }
            String subject = auth.getName(); // principal String puesto por MSJwtAuthFilter
            var roles = auth.getAuthorities().stream().map(Object::toString).toList();
            var decoded = decodeJwtPayload(req.getHeader("Authorization")); // solo debug

            System.out.println("[ms-production][AUTH] sub=" + subject + " roles=" + roles);

            Map<String, Object> out = new LinkedHashMap<>();
            out.put("service", "ms-production");
            out.put("subject", subject);
            out.put("authorities", roles);
            out.put("jwtPayloadDebug", decoded); // puede ser null si no hay header
            return out;
        }

        // ---- Utilidad de debug: decodifica el payload del JWT sin verificar la firma ----
        private static Map<String, Object> decodeJwtPayload(String authHeader) {
            try {
                if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
                String token = authHeader.substring("Bearer ".length());
                String[] parts = token.split("\\.");
                if (parts.length < 2) return null;
                String payload = new String(Base64.getUrlDecoder().decode(parts[1]), StandardCharsets.UTF_8);
                // parse muy simple a Map (sin libs JSON): formato fijo, lo devolvemos como String
                return Map.of("raw", payload);
            } catch (Exception e) {
                return Map.of("error", e.getMessage());
            }
        }
    }
}
