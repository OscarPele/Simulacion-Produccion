package com.oscar.proyecto.ms_auth.api.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @NotBlank @Email @Size(max = 120) String email, // coherente con @Column(length=120)
        @NotBlank @Size(min = 8, max = 128) String password
) {}
