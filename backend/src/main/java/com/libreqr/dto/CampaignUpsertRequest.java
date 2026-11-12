package com.libreqr.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * Entrada para crear o editar una campana (REQ-CAM-01).
 */
public record CampaignUpsertRequest(
        @NotNull(message = "name es obligatorio")
        @Size(min = 1, max = 120, message = "name debe tener entre 1 y 120 caracteres")
        String name,

        @Size(max = 500, message = "description no puede superar 500 caracteres")
        String description
) {
}
