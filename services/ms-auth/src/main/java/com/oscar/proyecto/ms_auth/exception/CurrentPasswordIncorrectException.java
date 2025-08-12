package com.oscar.proyecto.ms_auth.exception;

public class CurrentPasswordIncorrectException extends RuntimeException {
    public CurrentPasswordIncorrectException() {
        super("CURRENT_PASSWORD_INCORRECT");
    }
}
