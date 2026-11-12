package com.libreqr.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Salida de una campana. No expone ownerId: el servicio filtra por usuario autenticado.
 */
public record CampaignResponse(
        UUID id,
        String name,
        String description,
        Instant createdAt,
        Instant updatedAt
) {
}
