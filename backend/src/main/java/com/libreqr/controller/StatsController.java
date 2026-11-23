package com.libreqr.controller;

import com.libreqr.dto.CampaignScanStat;
import com.libreqr.dto.DailyScanStat;
import com.libreqr.dto.StatsSummaryResponse;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
import com.libreqr.service.StatsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Resumen global del dashboard (una unica llamada autenticada; solo datos propios).
 */
@RestController
@RequestMapping("/api/stats")
public class StatsController {

    private final StatsService stats;

    public StatsController(StatsService stats) {
        this.stats = stats;
    }

    @GetMapping("/summary")
    public StatsSummaryResponse summary(@CurrentUser AuthenticatedUser current) {
        return stats.summary(current.userId());
    }

    @GetMapping("/daily-scans")
    public List<DailyScanStat> dailyScans(@CurrentUser AuthenticatedUser current) {
        return stats.dailyScans(current.userId());
    }

    @GetMapping("/campaign-scans")
    public List<CampaignScanStat> campaignScans(@CurrentUser AuthenticatedUser current) {
        return stats.campaignScans(current.userId());
    }
}
