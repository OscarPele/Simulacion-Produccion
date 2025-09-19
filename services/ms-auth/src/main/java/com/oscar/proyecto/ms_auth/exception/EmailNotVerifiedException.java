package com.oscar.proyecto.ms_auth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class EmailNotVerifiedException extends ResponseStatusException {
    public EmailNotVerifiedException() { super(HttpStatus.FORBIDDEN, "EMAIL_NOT_VERIFIED"); }
}
