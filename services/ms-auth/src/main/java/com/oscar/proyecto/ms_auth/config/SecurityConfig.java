package com.oscar.proyecto.ms_auth.config;

import com.oscar.shared.security.MSSecurityConfig;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(MSSecurityConfig.class)
public class SecurityConfig {
}
