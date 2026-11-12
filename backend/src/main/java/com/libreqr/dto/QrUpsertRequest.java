package com.libreqr.dto;

import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.Map;
import java.util.UUID;

/**
 * Entrada para crear o editar un QR (REQ-QR-01..03, REQ-QR-06).
 *
 * @param type        tipo de QR (obligatorio)
 * @param name        nombre reconocible (obligatorio)
 * @param description descripcion opcional
 * @param payload     destino o contenido segun el tipo (obligatorio; formato validado
 *                    por el servicio segun el tipo)
 * @param designConfig diseno personalizable: marco, forma, logo (opcional)
 */
public record QrUpsertRequest(
        @NotNull(message = "type es obligatorio")
        QrType type,

        @NotNull(message = "name es obligatorio")
        @Size(min = 1, max = 120, message = "name debe tener entre 1 y 120 caracteres")
        String name,

        @Size(max = 500, message = "description no puede superar 500 caracteres")
        String description,

        @NotNull(message = "payload es obligatorio")
        @Size(min = 1, max = 4000, message = "payload debe tener entre 1 y 4000 caracteres")
        String payload,

        Map<String, Object> designConfig
) {
}
