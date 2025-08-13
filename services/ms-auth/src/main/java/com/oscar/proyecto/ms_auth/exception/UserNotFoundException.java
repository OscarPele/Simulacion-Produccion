package com.oscar.proyecto.ms_auth.exception;

public class UserNotFoundException extends RuntimeException {
    public UserNotFoundException() { super("USER_NOT_FOUND"); }
}
