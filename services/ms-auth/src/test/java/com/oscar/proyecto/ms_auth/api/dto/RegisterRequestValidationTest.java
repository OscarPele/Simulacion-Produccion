package com.oscar.proyecto.ms_auth.api.dto;

import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ConstraintViolation;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

class RegisterRequestValidationTest {
    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void invalid_fields_trigger_violations() {
        var dto = new RegisterRequest();
        dto.setUsername("");
        dto.setEmail("not-an-email");
        dto.setPassword("123");

        Set<ConstraintViolation<RegisterRequest>> v = validator.validate(dto);
        assertFalse(v.isEmpty());
    }

    @Test
    void valid_fields_pass() {
        var dto = new RegisterRequest();
        dto.setUsername("alice");
        dto.setEmail("alice@mail.com");
        dto.setPassword("Secret123");

        Set<ConstraintViolation<RegisterRequest>> v = validator.validate(dto);
        assertTrue(v.isEmpty());
    }
}
