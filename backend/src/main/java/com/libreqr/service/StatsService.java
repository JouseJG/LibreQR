package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignQr;
import com.libreqr.document.CampaignStatus;
import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.ScanEvent;
import com.libreqr.dto.CampaignScanStat;
import com.libreqr.dto.CampaignStatsResponse;
import com.libreqr.dto.CampaignStatsResponse.CategoryCount;
import com.libreqr.dto.DailyScanStat;
import com.libreqr.dto.QrStatsResponse;
import com.libreqr.dto.StatsSummaryResponse;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.repository.ScanEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
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
import java.util.function.Function;

/**
 * Resumen global del dashboard (una sola peticion).
 *
 * <p>Rango de medicion opcional [from, to] inclusivos; por defecto el mes natural en
 * curso. La comparativa usa el periodo anterior de IGUAL duracion terminado donde
 * empieza el actual (REQ-MET-05). Los bots quedan fuera del recuento principal; los
 * visitantes unicos son la estimacion por hash de cookie (REQ-MET-02).</p>
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

    /** Ventana efectiva: inclusiva por dias, con su periodo anterior equivalente. */
    private record Window(Instant from, Instant to, Instant prevFrom, Instant prevTo,
                          LocalDate firstDay, LocalDate lastDay) {
    }

    private Window resolveWindow(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(ZoneOffset.UTC);
        LocalDate first = from != null ? from : YearMonth.from(today).atDay(1);
        LocalDate last = to != null ? to : today;
        if (last.isBefore(first)) {
            last = first;
        }
        Instant start = first.atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant end = last.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();
        Instant prevTo = start;
        Instant prevFrom = end.minus(Duration.between(start, end));
        return new Window(start, end, prevFrom, prevTo, first, last);
    }

    public StatsSummaryResponse summary(UUID ownerId) {
        return summary(ownerId, null, null);
    }

    public StatsSummaryResponse summary(UUID ownerId, LocalDate from, LocalDate to) {
        Window window = resolveWindow(from, to);
        List<QrCode> ownedQrs = qrCodes.findByOwnerIdAndDeletedAtIsNull(ownerId);
        List<UUID> qrIds = ownedQrs.stream().map(QrCode::getId).toList();
        long activeQrs = ownedQrs.stream().filter(q -> q.getStatus() == QrStatus.active).count();

        List<Campaign> ownedCampaigns = campaigns.findByOwnerIdAndDeletedAtIsNull(ownerId);
        long activeCampaigns = ownedCampaigns.stream()
                .filter(c -> c.getStatus() == CampaignStatus.active)
                .count();

        long[] current = countScansAndUnique(qrIds, window.from, window.to);
        long[] previous = countScansAndUnique(qrIds, window.prevFrom, window.prevTo);

        return new StatsSummaryResponse(
                current[0], previous[0], current[1], previous[1],
                activeQrs, ownedQrs.size(), activeCampaigns, ownedCampaigns.size());
    }

    /** Serie diaria de escaneos humanos del rango (dias sin datos = 0). */
    public List<DailyScanStat> dailyScans(UUID ownerId, LocalDate from, LocalDate to) {
        Window window = resolveWindow(from, to);

        Map<LocalDate, Long> perDay = new HashMap<>();
        for (ScanEvent event : humanEventsIn(ownerId, window.from, window.to)) {
            perDay.merge(event.getScannedAt().atZone(ZoneOffset.UTC).toLocalDate(), 1L, Long::sum);
        }

        List<DailyScanStat> series = new ArrayList<>();
        for (LocalDate day = window.firstDay; !day.isAfter(window.lastDay); day = day.plusDays(1)) {
            series.add(new DailyScanStat(day, perDay.getOrDefault(day, 0L)));
        }
        return series;
    }

    /** Escaneos humanos del rango agrupados por campana (desc por volumen). */
    public List<CampaignScanStat> campaignScans(UUID ownerId, LocalDate from, LocalDate to) {
        Window window = resolveWindow(from, to);
        Map<UUID, Long> scansByQr = new HashMap<>();
        for (ScanEvent event : humanEventsIn(ownerId, window.from, window.to)) {
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

    /**
     * Estadisticas completas del perfil de campana para un rango: resumen, serie
     * diaria y agrupaciones por dispositivo/pais (REQ-MET-05..09).
     */
    public CampaignStatsResponse campaignStats(UUID ownerId, UUID campaignId,
                                               LocalDate from, LocalDate to) {
        Window window = resolveWindow(from, to);
        List<UUID> linkedIds = campaignLinks.findByCampaignId(campaignId).stream()
                .map(CampaignQr::getQrCodeId)
                .toList();
        List<QrCode> ownedQrs = linkedIds.isEmpty() ? List.of()
                : qrCodes.findByIdIn(linkedIds).stream()
                        .filter(q -> q.getOwnerId().equals(ownerId) && q.getDeletedAt() == null)
                        .toList();
        List<UUID> qrIds = ownedQrs.stream().map(QrCode::getId).toList();

        List<ScanEvent> human = qrIds.isEmpty() ? List.of()
                : scanEvents.findByQrCodeIdInAndScannedAtBetween(qrIds, window.from, window.to)
                        .stream().filter(e -> !e.isIsBot()).toList();

        long unique = human.stream()
                .map(ScanEvent::getVisitorHash)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long allTime = qrIds.isEmpty() ? 0
                : scanEvents.findByQrCodeIdIn(qrIds).stream().filter(e -> !e.isIsBot()).count();

        long activeQrs = ownedQrs.stream().filter(q -> q.getStatus() == QrStatus.active).count();

        return new CampaignStatsResponse(
                human.size(), unique, allTime, activeQrs, ownedQrs.size(),
                dailySeries(human, window),
                groupCount(human, ScanEvent::getDeviceType),
                groupCount(human, ScanEvent::getCountryCode));
    }

    /** Estadisticas del perfil de un QR (REQ-MET-03..09); 404 si no es propio. */
    public QrStatsResponse qrStats(UUID ownerId, UUID qrId, LocalDate from, LocalDate to) {
        QrCode owned = qrCodes.findById(qrId)
                .filter(q -> q.getDeletedAt() == null && q.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("QR no encontrado: " + qrId));

        Window window = resolveWindow(from, to);
        List<ScanEvent> events = scanEvents
                .findByQrCodeIdAndScannedAtBetween(owned.getId(), window.from, window.to);
        List<ScanEvent> human = events.stream().filter(e -> !e.isIsBot()).toList();

        long unique = human.stream()
                .map(ScanEvent::getVisitorHash)
                .filter(Objects::nonNull)
                .distinct()
                .count();
        long allTime = scanEvents.findByQrCodeId(owned.getId()).stream()
                .filter(e -> !e.isIsBot())
                .count();

        return new QrStatsResponse(
                human.size(), unique, allTime, events.size() - human.size(),
                dailySeries(human, window),
                groupCount(human, ScanEvent::getDeviceType),
                groupCount(human, ScanEvent::getCountryCode));
    }

    /** Serie diaria en CSV (descarga de los perfiles de campana y QR). */
    public static String toCsv(List<DailyScanStat> series) {
        StringBuilder csv = new StringBuilder("fecha,escaneos\n");
        for (DailyScanStat point : series) {
            csv.append(point.date()).append(',').append(point.scans()).append('\n');
        }
        return csv.toString();
    }

    /** Serie diaria completa del rango (dias sin datos = 0). */
    private List<DailyScanStat> dailySeries(List<ScanEvent> human, Window window) {
        Map<LocalDate, Long> perDay = new HashMap<>();
        for (ScanEvent event : human) {
            perDay.merge(event.getScannedAt().atZone(ZoneOffset.UTC).toLocalDate(), 1L, Long::sum);
        }
        List<DailyScanStat> series = new ArrayList<>();
        for (LocalDate day = window.firstDay; !day.isAfter(window.lastDay); day = day.plusDays(1)) {
            series.add(new DailyScanStat(day, perDay.getOrDefault(day, 0L)));
        }
        return series;
    }

    /** Agrupa eventos humanos por una clave categorica desc; null se normaliza a unknown. */
    private List<CategoryCount> groupCount(List<ScanEvent> events,
                                           Function<ScanEvent, String> keyFn) {
        Map<String, Long> counts = new HashMap<>();
        for (ScanEvent event : events) {
            String key = keyFn.apply(event);
            counts.merge(key == null ? "unknown" : key, 1L, Long::sum);
        }
        return counts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .map(entry -> new CategoryCount(entry.getKey(), entry.getValue()))
                .toList();
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
