package com.libreqr.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Estadisticas del perfil de un QR para un rango (REQ-MET-03..09). Mismo enfoque que
 * `CampaignStatsResponse`: escaneos humanos, serie diaria completa y agrupaciones
 * por dispositivo/pais (null -> unknown). Anade los bots del rango, relevantes al
 * medir un unico codigo.
 */
public record QrStatsResponse(
        long scans,
        long uniqueVisitors,
        long scansAllTime,
        long botScans,
        List<DailyScanStat> dailySeries,
        List<CampaignStatsResponse.CategoryCount> deviceTypes,
        List<CampaignStatsResponse.CategoryCount> countries) {
}
