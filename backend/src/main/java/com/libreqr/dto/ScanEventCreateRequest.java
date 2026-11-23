package com.libreqr.dto;

import java.time.Instant;

/**
 * Datos de un escaneo. No se incluye visitorKeyHash para no exponer claves internas.
 */
public record ScanEventCreateRequest(
        Instant scannedAt,
        String countryCode,
        String region,
        String city,
        String deviceType,
        String operatingSystem,
        String browser
) {
}
