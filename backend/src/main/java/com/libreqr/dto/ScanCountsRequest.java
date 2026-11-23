package com.libreqr.dto;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.util.List;
import java.util.UUID;

/**
 * Entrada para el conteo de escaneos por lotes (lista del frontend: una peticion para
 * toda la pagina en lugar de una por QR).
 *
 * @param ids QRs a contar (maximo 100 por peticion)
 */
public record ScanCountsRequest(
        @NotEmpty(message = "ids no puede estar vacio")
        @Size(max = 100, message = "maximo 100 ids por peticion")
        List<UUID> ids
) {
}
