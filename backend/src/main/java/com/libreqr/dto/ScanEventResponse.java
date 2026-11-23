package com.libreqr.dto;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de escaneo devuelto por la API (REQ-MET-03).
 */
public record ScanEventResponse(
        UUID id,
        UUID qrCodeId,
        Instant scannedAt,
        String countryCode,
        String region,
        String city,
        String deviceType,
        String operatingSystem,
        String browser,
        boolean isBot,
        Instant createdAt
) {
}
