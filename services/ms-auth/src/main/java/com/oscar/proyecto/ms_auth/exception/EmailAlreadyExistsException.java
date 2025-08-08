package com.oscar.proyecto.ms_auth.exception;

public class EmailAlreadyExistsException extends RuntimeException {
    public EmailAlreadyExistsException() {
        super("EMAIL_EXISTS");
    }
}
