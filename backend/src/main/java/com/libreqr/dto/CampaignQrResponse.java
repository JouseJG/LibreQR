package com.libreqr.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Relacion campana-QR devuelta por la API (REQ-CAM-03).
 */
public record CampaignQrResponse(
        UUID campaignId,
        UUID qrCodeId,
        Instant addedAt
) {
}
