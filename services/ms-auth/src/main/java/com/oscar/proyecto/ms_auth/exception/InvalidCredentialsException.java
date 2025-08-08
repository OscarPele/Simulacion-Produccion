package com.oscar.proyecto.ms_auth.exception;

public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("INVALID_CREDENTIALS");
    }
}
