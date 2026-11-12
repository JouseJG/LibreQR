package com.libreqr.exception;

import org.springframework.http.HttpStatus;

/**
 * Excepcion base de dominio con su codigo y su estado HTTP.
 *
 * <p>{@link #httpStatus()} es publico para que el manejador global ({@code
 * GlobalExceptionHandler}) pueda mapear la excepcion a la respuesta correspondiente. El
 * codigo interno y los detalles técnicos quedan encapsulados y no se exponen en la API.</p>
 */
public abstract class ApiDomainException extends RuntimeException {

    private final String code;

    protected ApiDomainException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }

    /** Estado HTTP que representa este error de dominio (no se expone como campo JSON). */
    public abstract HttpStatus httpStatus();
}
