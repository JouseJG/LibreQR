package com.libreqr.controller;

import com.libreqr.dto.QrMetricsResponse;
import com.libreqr.dto.QrStatsResponse;
import com.libreqr.dto.DailyScanStat;
import com.libreqr.dto.ScanCountsRequest;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
import com.libreqr.service.MetricsService;
import com.libreqr.service.StatsService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Metricas de un QR (REQ-MET-03). Las fechas se aceptan en formato ISO-8601
 * (p. ej. 2026-11-01T00:00:00Z); si no se indican, el servicio usa los ultimos 30 dias.
 */
@RestController
@RequestMapping("/api/qr")
public class QrMetricsController {

    private final MetricsService metrics;
    private final StatsService stats;

    public QrMetricsController(MetricsService metrics, StatsService stats) {
        this.metrics = metrics;
        this.stats = stats;
    }

    @GetMapping("/{id}/metrics")
    public QrMetricsResponse metrics(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
            Instant to) {
        return metrics.qrMetrics(current.userId(), id, from, to);
    }

    @GetMapping("/{id}/stats")
    public QrStatsResponse stats(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return stats.qrStats(current.userId(), id, from, to); // 404 si no es propio
    }

    /** Serie diaria del rango en CSV (descarga desde el perfil del QR). */
    @GetMapping(value = "/{id}/stats/export", produces = "text/csv")
    public org.springframework.http.ResponseEntity<String> exportStats(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        QrStatsResponse data = stats.qrStats(current.userId(), id, from, to);
        List<DailyScanStat> series = data.dailySeries();
        LocalDate first = series.isEmpty() ? LocalDate.now() : series.get(0).date();
        LocalDate last = series.isEmpty() ? LocalDate.now()
                : series.get(series.size() - 1).date();
        return org.springframework.http.ResponseEntity.ok()
                .header("Content-Disposition",
                        "attachment; filename=\"qr-%s-%s_a_%s.csv\"".formatted(id, first, last))
                .body(StatsService.toCsv(series));
    }

    /**
     * Conteo de escaneos humanos por lotes para las listas (una peticion por pagina).
     * Devuelve un mapa id -> escaneos; los QRs sin escaneos aparecen con 0, los ajenos
     * se omiten.
     */
    @PostMapping("/scan-counts")
    public Map<String, Long> scanCounts(
            @CurrentUser AuthenticatedUser current,
            @Valid @RequestBody ScanCountsRequest request) {
        Map<String, Long> out = new java.util.LinkedHashMap<>();
        metrics.humanScanCounts(current.userId(), request.ids())
                .forEach((id, count) -> out.put(id.toString(), count));
        return out;
    }
}
