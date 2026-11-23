package com.libreqr.security;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marca un parametro {@link CurrentUser} en un controlador. El {@code
 * CurrentUserArgumentResolver} lo inyecta con el usuario autenticado de la request.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
public @interface CurrentUser {
}
