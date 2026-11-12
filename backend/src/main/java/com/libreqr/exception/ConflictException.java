package com.libreqr.exception;

/**
 * Accion no permitida con el estado actual del recurso (HTTP 409).
 */
public class ConflictException extends ApiDomainException {

    public ConflictException(String message) {
        super("CONFLICT", message);
    }

    @Override
    public org.springframework.http.HttpStatus httpStatus() {
        return org.springframework.http.HttpStatus.CONFLICT;
    }
}
