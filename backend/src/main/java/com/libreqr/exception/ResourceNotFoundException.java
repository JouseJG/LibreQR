package com.libreqr.exception;

import org.springframework.http.HttpStatus;

/**
 * Recurso inexistente o borrado logicamente.
 */
public class ResourceNotFoundException extends ApiDomainException {

    public ResourceNotFoundException(String message) {
        super("NOT_FOUND", message);
    }

    @Override
    public HttpStatus httpStatus() {
        return HttpStatus.NOT_FOUND;
    }
}
