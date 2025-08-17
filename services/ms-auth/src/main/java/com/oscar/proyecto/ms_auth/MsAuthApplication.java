package com.oscar.proyecto.ms_auth;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.security.Principal;
import java.time.Instant;
import java.util.Map;

@EnableScheduling
@SpringBootApplication
public class MsAuthApplication {

    public static void main(String[] args) {
        SpringApplication.run(MsAuthApplication.class, args);
    }
    @RestController
    static class TestController {
        @GetMapping(path = "/test/protected")
        public Map<String, Object> protectedPing(Principal principal) {
            return Map.of(
                    "ok", true,
                    "user", principal != null ? principal.getName() : null,
                    "ts", Instant.now().toString(),
                    "message", "ms-auth protected OK. PING DE CAMBIO, COMMIT Y MERGE"
            );
        }
    }
}
