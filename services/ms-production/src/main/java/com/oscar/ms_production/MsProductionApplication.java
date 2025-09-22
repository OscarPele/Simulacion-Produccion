package com.oscar.ms_production;

import com.oscar.shared.security.MSSecurityConfig;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Import;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@SpringBootApplication
@Import(MSSecurityConfig.class)
public class MsProductionApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsProductionApplication.class, args);
    }

    @RestController
    @RequestMapping("/api/production")
    static class ProductionController {

        @GetMapping("/public/ping")
        public Map<String, Object> publicPing() {
            return Map.of(
                    "service", "ms-production",
                    "status", "ok",
                    "timestamp", Instant.now().toString()
            );
        }

        @GetMapping("/secure/me")
        public Map<String, Object> secureMe(Authentication auth) {
            var out = new LinkedHashMap<String, Object>();
            out.put("service", "ms-production");
            out.put("subject", auth.getName()); // principal String puesto por MSJwtAuthFilter
            out.put("authorities", auth.getAuthorities().stream().map(Object::toString).toList());
            return out;
        }
    }
}
