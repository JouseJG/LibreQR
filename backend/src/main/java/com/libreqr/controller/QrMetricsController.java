package com.libreqr.controller;

import com.libreqr.dto.QrMetricsResponse;
import com.libreqr.dto.ScanCountsRequest;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
import com.libreqr.service.MetricsService;
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

    public QrMetricsController(MetricsService metrics) {
        this.metrics = metrics;
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
