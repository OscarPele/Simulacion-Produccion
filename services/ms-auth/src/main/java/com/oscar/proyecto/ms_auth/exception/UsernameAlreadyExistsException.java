package com.oscar.proyecto.ms_auth.exception;

public class UsernameAlreadyExistsException extends RuntimeException {
    public UsernameAlreadyExistsException() {
        super("USERNAME_EXISTS");
    }
}
