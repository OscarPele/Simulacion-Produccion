package com.oscar.proyecto.ms_auth.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class LoginRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void blank_is_invalid() {
        var dto = new LoginRequest();
        dto.setUsernameOrEmail("");
        dto.setPassword("");
        assertFalse(validator.validate(dto).isEmpty());
    }

    @Test
    void ok_when_filled() {
        var dto = new LoginRequest();
        dto.setUsernameOrEmail("alice");
        dto.setPassword("Secret123");
        assertTrue(validator.validate(dto).isEmpty());
    }
}
