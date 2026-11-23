package com.libreqr.dto;

import java.time.Instant;
import java.util.Map;

/**
 * Metricas de un QR individual (REQ-MET-03).
 *
 * <p>Los datos no disponibles se devuelven null/vacios: la interfaz no debe inventar
 * (REQ-MET-07). Las fechas del periodo consultado se devuelven para que el cliente
 * etiquete la ventana temporal.</p>
 */
public record QrMetricsResponse(
        long totalScans,
        long humanScans,
        long botScans,
        long uniqueVisitors,
        double dailyAverage,
        String busiestDay,          // yyyy-MM-dd o null si no hay datos
        Integer busiestHour,         // 0-23 UTC o null
        Instant from,
        Instant to,
        Instant firstScanAt,
        Instant lastScanAt,
        long previousPeriodScans,    // scans del periodo anterior equivalente
        Map<String, Long> byCountry, // pais aproximado -> escaneos humanos
        Map<String, Long> byDevice   // dispositivo -> escaneos humanos
) {
}
