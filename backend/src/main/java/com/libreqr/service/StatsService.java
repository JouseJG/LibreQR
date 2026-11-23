package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignStatus;
import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.ScanEvent;
import com.libreqr.dto.CampaignScanStat;
import com.libreqr.dto.DailyScanStat;
import com.libreqr.dto.StatsSummaryResponse;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.repository.ScanEventRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Resumen global del dashboard (una sola peticion).
 *
 * <p>Comparativa mensual: mes natural en curso frente al mismo tramo del mes
 * anterior (misma duracion, REQ-MET-05). Los escaneos de bots quedan fuera del
 * recuento principal; los visitantes unicos son la estimacion por hash de cookie
 * ya documentada (REQ-MET-02).</p>
 */
@Service
public class StatsService {

    private final QrCodeRepository qrCodes;
    private final CampaignRepository campaigns;
    private final CampaignQrRepository campaignLinks;
    private final ScanEventRepository scanEvents;

    public StatsService(QrCodeRepository qrCodes, CampaignRepository campaigns,
                        CampaignQrRepository campaignLinks, ScanEventRepository scanEvents) {
        this.qrCodes = qrCodes;
        this.campaigns = campaigns;
        this.campaignLinks = campaignLinks;
        this.scanEvents = scanEvents;
    }

    /** Serie diaria de escaneos humanos del mes natural en curso (dias sin datos = 0). */
    public List<DailyScanStat> dailyScans(UUID ownerId) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth month = YearMonth.from(today);
        Instant from = month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant to = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        Map<LocalDate, Long> perDay = new HashMap<>();
        for (ScanEvent event : humanEventsIn(ownerId, from, to)) {
            perDay.merge(event.getScannedAt().atZone(ZoneOffset.UTC).toLocalDate(), 1L, Long::sum);
        }

        List<DailyScanStat> series = new ArrayList<>();
        for (LocalDate day = month.atDay(1); !day.isAfter(today); day = day.plusDays(1)) {
            series.add(new DailyScanStat(day, perDay.getOrDefault(day, 0L)));
        }
        return series;
    }

    /** Escaneos humanos del mes en curso agrupados por campana (desc por volumen). */
    public List<CampaignScanStat> campaignScans(UUID ownerId) {
        Instant[] window = currentMonthWindow();
        Map<UUID, Long> scansByQr = new HashMap<>();
        for (ScanEvent event : humanEventsIn(ownerId, window[0], window[1])) {
            scansByQr.merge(event.getQrCodeId(), 1L, Long::sum);
        }

        List<Campaign> ownedCampaigns = campaigns.findByOwnerIdAndDeletedAtIsNull(ownerId);
        Map<UUID, Long> byCampaign = new HashMap<>();
        for (Campaign campaign : ownedCampaigns) {
            long sum = campaignLinks.findByCampaignId(campaign.getId()).stream()
                    .mapToLong(link -> scansByQr.getOrDefault(link.getQrCodeId(), 0L))
                    .sum();
            byCampaign.put(campaign.getId(), sum);
        }

        return ownedCampaigns.stream()
                .map(c -> new CampaignScanStat(c.getId(), c.getName(),
                        byCampaign.getOrDefault(c.getId(), 0L)))
                .sorted(Comparator.comparingLong(CampaignScanStat::scans).reversed())
                .toList();
    }

    private Instant[] currentMonthWindow() {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth month = YearMonth.from(today);
        return new Instant[] {
                month.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
                today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant(),
        };
    }

    private List<ScanEvent> humanEventsIn(UUID ownerId, Instant from, Instant to) {
        List<UUID> qrIds = qrCodes.findByOwnerIdAndDeletedAtIsNull(ownerId).stream()
                .map(QrCode::getId)
                .toList();
        if (qrIds.isEmpty()) {
            return List.of();
        }
        return scanEvents.findByQrCodeIdInAndScannedAtBetween(qrIds, from, to).stream()
                .filter(e -> !e.isIsBot())
                .toList();
    }

    public StatsSummaryResponse summary(UUID ownerId) {
        List<QrCode> ownedQrs = qrCodes.findByOwnerIdAndDeletedAtIsNull(ownerId);
        List<UUID> qrIds = ownedQrs.stream().map(QrCode::getId).toList();
        long activeQrs = ownedQrs.stream().filter(q -> q.getStatus() == QrStatus.active).count();

        List<Campaign> ownedCampaigns = campaigns.findByOwnerIdAndDeletedAtIsNull(ownerId);
        long activeCampaigns = ownedCampaigns.stream()
                .filter(c -> c.getStatus() == CampaignStatus.active)
                .count();

        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        YearMonth currentMonth = YearMonth.from(today);
        Instant currentFrom = currentMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant currentTo = today.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

        YearMonth previousMonth = currentMonth.minusMonths(1);
        Instant previousFrom = previousMonth.atDay(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant previousTo = today.minusMonths(1).plusDays(1)
                .atStartOfDay(ZoneOffset.UTC).toInstant();

        long[] current = countScansAndUnique(qrIds, currentFrom, currentTo);
        long[] previous = countScansAndUnique(qrIds, previousFrom, previousTo);

        return new StatsSummaryResponse(
                current[0], previous[0], current[1], previous[1],
                activeQrs, ownedQrs.size(), activeCampaigns, ownedCampaigns.size());
    }

    /** [escaneos humanos, visitantes unicos] en el periodo dado. */
    private long[] countScansAndUnique(List<UUID> qrIds, Instant from, Instant to) {
        if (qrIds.isEmpty()) {
            return new long[] {0, 0};
        }
        List<ScanEvent> human = scanEvents
                .findByQrCodeIdInAndScannedAtBetween(qrIds, from, to).stream()
                .filter(e -> !e.isIsBot())
                .toList();
        long unique = human.stream()
                .map(ScanEvent::getVisitorHash)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        return new long[] {human.size(), unique};
    }
}
