package com.libreqr.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Metricas agregadas de una campana (REQ-MET-04).
 *
 * <p>Los visitantes unicos se calculan como identificadores distintos entre TODOS los QRs
 * de la campana, no sumando los de cada QR (criterio tecnico del documento).</p>
 */
public record CampaignMetricsResponse(
        long totalScans,
        long humanScans,
        long botScans,
        long uniqueVisitors,
        double dailyAverage,
        Instant from,
        Instant to,
        long previousPeriodScans,
        MostScanned mostScanned,
        double activeWithScansPercent, // % de QRs activos que recibieron escaneos
        Map<String, Long> byCountry,
        Map<String, Long> byDevice
) {

    /** QR mas y menos escaneado de la campana (REQ-MET-04). */
    public record MostScanned(String mostName, long mostScans, String leastName, long leastScans) {
    }
}
