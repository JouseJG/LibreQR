package com.libreqr.exception;

import org.springframework.http.HttpStatus;

/**
 * Validacion de negocio fallida (REQ-QR-02).
 */
public class InvalidContentException extends ApiDomainException {

    public InvalidContentException(String message) {
        super("INVALID_CONTENT", message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.BAD_REQUEST;
    }
}
