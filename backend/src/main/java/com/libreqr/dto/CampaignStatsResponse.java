package com.libreqr.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Estadisticas del perfil de campana para un rango dado (REQ-MET-05..09).
 * Escaneos humanos (sin bots); visitantes unicos = hashes de cookie distintos
 * (estimacion); deviceTypes/countries agrupados desc; dailySeries incluye todos
 * los dias del rango (sin datos = 0). Las claves null de dispositivo/pais se
 * normalizan a "unknown" (REQ-MET-07: no inventar, pero si contabilizar).
 */
public record CampaignStatsResponse(
        long scans,
        long uniqueVisitors,
        long scansAllTime,
        long activeQrs,
        long totalQrs,
        List<DailyScanStat> dailySeries,
        List<CategoryCount> deviceTypes,
        List<CategoryCount> countries) {

    public record CategoryCount(String key, long scans) {
    }
}
