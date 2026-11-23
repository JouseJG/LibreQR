package com.libreqr.dto;

import com.libreqr.document.CampaignStatus;
import jakarta.validation.constraints.NotNull;

/**
 * Entrada para cambiar el estado de una campana (pausar/reactivar/finalizar).
 */
public record CampaignStatusRequest(
        @NotNull(message = "status es obligatorio")
        CampaignStatus status
) {
}
