package com.libreqr.dto;

import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Salida de un QR. No expone ownerId: el acceso se filtra por usuario en el servicio.
 *
 * <p>{@code editable} indica si el destino puede cambiarse sin regenerar la imagen
 * (REQ-QR-07: solo en QRs dinamicos tipo url).</p>
 */
public record QrResponse(
        UUID id,
        QrType type,
        QrStatus status,
        String name,
        String description,
        String slug,
        String payload,
        UUID campaignId,
        Map<String, Object> designConfig,
        String imageUrl,
        boolean editable,
        Instant createdAt,
        Instant updatedAt
) {
}
