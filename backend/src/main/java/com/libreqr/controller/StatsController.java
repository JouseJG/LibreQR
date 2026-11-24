package com.libreqr.controller;

import com.libreqr.dto.CampaignScanStat;
import com.libreqr.dto.DailyScanStat;
import com.libreqr.dto.StatsSummaryResponse;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
import com.libreqr.service.StatsService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

/**
 * Resumen del dashboard (autenticado; solo datos propios). Los tres endpoints aceptan
 * el rango opcional `from`/`to` (ISO date, inclusivo); por defecto, mes en curso.
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/summary")
    public StatsSummaryResponse summary(
            @CurrentUser AuthenticatedUser current,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return stats.summary(current.userId(), from, to);
    }

    @GetMapping("/daily-scans")
    public List<DailyScanStat> dailyScans(
            @CurrentUser AuthenticatedUser current,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return stats.dailyScans(current.userId(), from, to);
    }

    @GetMapping("/campaign-scans")
    public List<CampaignScanStat> campaignScans(
            @CurrentUser AuthenticatedUser current,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            LocalDate to) {
        return stats.campaignScans(current.userId(), from, to);
    }
}
