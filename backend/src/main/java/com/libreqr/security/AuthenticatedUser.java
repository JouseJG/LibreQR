package com.libreqr.security;

import java.util.UUID;

/**
 * Usuario autenticado de la peticion actual (resuelto desde la cookie de sesion o la
 * API key Bearer; nunca desde un id enviado por el cliente).
 */
public record AuthenticatedUser(UUID userId) {
}
