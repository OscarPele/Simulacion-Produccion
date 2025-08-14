package com.oscar.proyecto.ms_auth.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI msAuthOpenAPI() {
        final String bearerKey = "bearer-jwt";

        return new OpenAPI()
                .info(new Info()
                        .title("ms-auth · API")
                        .version("1.0.0")
                        .description("""
                            API de autenticación y gestión de usuarios:
                            - Registro y autenticación con emisión de JWT (RS256)
                            - Gestión segura de refresh tokens con rotación y revocación
                            - Cambio de contraseña para usuarios autenticados
                            """)
                        .license(new License().name("MIT License")))
                .components(new Components()
                        .addSecuritySchemes(bearerKey, new SecurityScheme()
                                .name("Authorization")
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")))
                // Seguridad global (se ignora en endpoints permit-all)
                .addSecurityItem(new SecurityRequirement().addList(bearerKey));
    }
}
