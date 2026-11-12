package com.libreqr.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

/**
 * Traduccion centralizada de excepciones de dominio a respuestas RFC 7807 (ProblemDetail),
 * aplicando el estado HTTP que declara cada excepcion.
 *
 * <p>Cumple el criterio de calidad: respuestas coherentes y sin secretos ni informacion
 * interna del servidor. El {@code type} de la respuesta identifica el error de forma
 * estable para el cliente.</p>
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(ApiDomainException.class)
    public ProblemDetail handleDomain(ApiDomainException ex) {
        HttpStatus status = ex.httpStatus();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(status, ex.getMessage());
        pd.setType(java.net.URI.create("urn:libreqr:error:" + ex.getCode().toLowerCase()));
        pd.setTitle(status.getReasonPhrase());
        return pd;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidation(MethodArgumentNotValidException ex) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(e -> e.getField() + ": " + e.getDefaultMessage())
                .collect(Collectors.joining("; "));
        if (detail.isEmpty()) {
            detail = "Validacion fallida";
        }
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, detail);
        pd.setType(java.net.URI.create("urn:libreqr:error:validation"));
        pd.setTitle(HttpStatus.BAD_REQUEST.getReasonPhrase());
        return pd;
    }
}
