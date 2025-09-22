package com.oscar.proyecto.ms_hr;

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
public class MsHrApplication {
    public static void main(String[] args) {
        SpringApplication.run(MsHrApplication.class, args);
    }

    @RestController
    @RequestMapping("/api/hr")
    static class HrController {

        @GetMapping("/public/ping")
        public Map<String, Object> publicPing() {
            return Map.of(
                    "service", "ms-hr",
                    "status", "ok",
                    "timestamp", Instant.now().toString()
            );
        }

        @GetMapping("/secure/me")
        public Map<String, Object> secureMe(Authentication auth) {
            var out = new LinkedHashMap<String, Object>();
            out.put("service", "ms-hr");
            out.put("subject", auth.getName()); // principal String puesto por MSJwtAuthFilter
            out.put("authorities", auth.getAuthorities().stream().map(Object::toString).toList());
            return out;
        }
    }
}
