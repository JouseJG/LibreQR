package com.libreqr.dto;

import com.libreqr.document.CampaignStatus;

import java.time.Instant;
import java.util.UUID;

/**
 * Salida de una campana. No expone ownerId: el servicio filtra por usuario autenticado.
 */
public record CampaignResponse(
        UUID id,
        String name,
        String description,
        CampaignStatus status,
        Instant createdAt,
        Instant updatedAt
) {
}
