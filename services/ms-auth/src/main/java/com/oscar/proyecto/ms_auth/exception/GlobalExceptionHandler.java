package com.oscar.proyecto.ms_auth.exception;

import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


import java.util.Map;

@RestControllerAdvice(basePackages = "com.oscar.proyecto.ms_auth")
@Order(Ordered.HIGHEST_PRECEDENCE)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(UsernameAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleUsernameExists(UsernameAlreadyExistsException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(EmailAlreadyExistsException.class)
    public ResponseEntity<Map<String, String>> handleEmailExists(EmailAlreadyExistsException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    @ExceptionHandler(InvalidCredentialsException.class)
    public ResponseEntity<Map<String, String>> handleInvalidCredentials(InvalidCredentialsException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.UNAUTHORIZED, ex.getMessage());
    }

    @ExceptionHandler(CurrentPasswordIncorrectException.class)
    public ResponseEntity<Map<String, String>> handleCurrentPwd(CurrentPasswordIncorrectException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.BAD_REQUEST, ex.getMessage());
    }

    // NUEVOS: 404 y 403
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Map<String, String>> handleUserNotFound(UserNotFoundException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.NOT_FOUND, ex.getMessage()); // "USER_NOT_FOUND"
    }

    @ExceptionHandler(ForbiddenOperationException.class)
    public ResponseEntity<Map<String, String>> handleForbidden(ForbiddenOperationException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getMessage());
        return buildResponse(HttpStatus.FORBIDDEN, ex.getMessage()); // "FORBIDDEN"
    }

    // Si queda algún ResponseStatusException suelto, respétalo (evita que caiga al 500 genérico)
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<Map<String, String>> handleRse(ResponseStatusException ex) {
        System.out.println(ex.getClass().getName() + ": " + ex.getReason());
        String code = ex.getReason() != null ? ex.getReason() : ex.getStatusCode().toString();
        return buildResponse(HttpStatus.valueOf(ex.getStatusCode().value()), code);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, String>> handleValidationErrors(MethodArgumentNotValidException ex) {
        System.out.println(MethodArgumentNotValidException.class.getName() + ": VALIDATION_ERROR");
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR");
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, String>> handleGeneric(Exception ex) {
        log.error("GENERIC handler: {}", ex.toString(), ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "UNEXPECTED_ERROR");
    }


    private ResponseEntity<Map<String, String>> buildResponse(HttpStatus status, String message) {
        String safeMessage = (message == null || message.isBlank()) ? status.name() : message;
        return ResponseEntity.status(status).body(Map.of("code", safeMessage));
    }

}
