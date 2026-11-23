package com.libreqr.service;

import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.ScanEvent;
import com.libreqr.dto.CampaignMetricsResponse;
import com.libreqr.dto.QrMetricsResponse;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.repository.ScanEventRepository;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Metricas de QR y campanas (REQ-MET-02..09).
 *
 * <p>Las metricas humanas excluyen los bots. Los visitantes unicos son una ESTIMACION a
 * partir de hashes de cookie distintos: varia si se borran/bloquean cookies o se cambia de
 * dispositivo (limitacion documentada, REQ-MET-02). Los valores no disponibles se devuelven
 * null/vacios sin inventar (REQ-MET-07); sin visitas el resultado muestra ceros y fechas
 * null (estado vacio claro, REQ-MET-09).</p>
 */
@Service
public class MetricsService {

    private final ScanEventRepository scanEvents;
    private final QrCodeRepository qrCodes;
    private final CampaignQrRepository campaignQrs;

    public MetricsService(ScanEventRepository scanEvents,
                          QrCodeRepository qrCodes,
                          CampaignQrRepository campaignQrs) {
        this.scanEvents = scanEvents;
        this.qrCodes = qrCodes;
        this.campaignQrs = campaignQrs;
    }

    public QrMetricsResponse qrMetrics(UUID ownerId, UUID qrId, Instant from, Instant to) {
        QrCode qr = qrMetricsOwneq(ownerId, qrId);
        return qrMetricsInternal(qr.getId(), from, to);
    }

    private QrCode qrMetricsOwneq(UUID ownerId, UUID qrId) {
        // valida propiedad y borrado logico igual que QrService (404 si no es propio)
        QrCode qr = qrCodes.findById(qrId)
                .filter(q -> q.getDeletedAt() == null && q.getOwnerId().equals(ownerId))
                .orElseThrow(() ->
                        new com.libreqr.exception.ResourceNotFoundException("QR no encontrado: " + qrId));
        return qr;
    }

    private QrMetricsResponse qrMetricsInternal(UUID qrId, Instant from, Instant to) {
        Instant now = Instant.now();
        Instant periodFrom = from != null ? from : now.minus(Duration.ofDays(30));
        Instant periodTo = to != null ? to : now;
        Duration window = Duration.between(periodFrom, periodTo);
        Instant prevFrom = periodFrom.minus(window);
        Instant prevTo = periodFrom;

        List<ScanEvent> events = scanEvents
                .findByQrCodeIdAndScannedAtBetween(qrId, periodFrom, periodTo);
        List<ScanEvent> human = events.stream().filter(e -> !e.isIsBot()).toList();
        long bots = events.size() - human.size();
        long prevScans = scanEvents
                .findByQrCodeIdAndScannedAtBetween(qrId, prevFrom, prevTo).size();

        long unique = human.stream()
                .map(ScanEvent::getVisitorHash)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        String busiestDay = human.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> com.libreqr.util.ScanClassifier.dayKey(e.getScannedAt()),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Integer busiestHour = human.stream()
                .collect(java.util.stream.Collectors.groupingBy(
                        e -> com.libreqr.util.ScanClassifier.hourUtc(e.getScannedAt()),
                        java.util.stream.Collectors.counting()))
                .entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);

        Instant first = human.stream().map(ScanEvent::getScannedAt)
                .min(Comparator.naturalOrder()).orElse(null);
        Instant last = human.stream().map(ScanEvent::getScannedAt)
                .max(Comparator.naturalOrder()).orElse(null);

        double dayCount = Math.max(1, window.toDays() + 1);

        return new QrMetricsResponse(
                events.size(), human.size(), bots, unique,
                (double) human.size() / dayCount,
                busiestDay, busiestHour, periodFrom, periodTo, first, last, prevScans,
                groupBy(human, ScanEvent::getCountryCode),
                groupBy(human, ScanEvent::getDeviceType));
    }

    public CampaignMetricsResponse campaignMetrics(UUID ownerId, UUID campaignId,
                                                   Instant from, Instant to) {
        return campaignMetricsInternal(ownerId, campaignId, from, to);
    }

    private CampaignMetricsResponse campaignMetricsInternal(UUID ownerId, UUID campaignId,
                                                            Instant from, Instant to) {
        List<UUID> qrIds = campaignQrs.findByCampaignId(campaignId).stream()
                .map(l -> l.getQrCodeId())
                .toList();
        List<QrCode> qrList = qrCodes.findByIdIn(qrIds).stream()
                .filter(q -> q.getOwnerId().equals(ownerId) && q.getDeletedAt() == null)
                .toList();
        List<UUID> ownedIds = qrList.stream().map(QrCode::getId).toList();

        Instant now = Instant.now();
        Instant periodFrom = from != null ? from : now.minus(Duration.ofDays(30));
        Instant periodTo = to != null ? to : now;
        Duration window = Duration.between(periodFrom, periodTo);
        Instant prevFrom = periodFrom.minus(window);
        Instant prevTo = periodFrom;

        List<ScanEvent> events = ownedIds.isEmpty()
                ? List.of()
                : scanEvents.findByQrCodeIdInAndScannedAtBetween(ownedIds, periodFrom, periodTo);
        List<ScanEvent> human = events.stream().filter(e -> !e.isIsBot()).toList();
        long bots = events.size() - human.size();

        // visitantes unicos = hashes distintos entre TODOS los QRs, no suma de unicos por QR
        long unique = human.stream()
                .map(ScanEvent::getVisitorHash)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .count();

        List<ScanEvent> prevEvents = ownedIds.isEmpty()
                ? List.of()
                : scanEvents.findByQrCodeIdInAndScannedAtBetween(ownedIds, prevFrom, prevTo);

        // QR mas y menos escaneado (humanos)
        Map<UUID, Long> perQr = new HashMap<>();
        for (ScanEvent e : human) {
            perQr.merge(e.getQrCodeId(), 1L, Long::sum);
        }
        CampaignMetricsResponse.MostScanned most = qrList.isEmpty()
                ? null
                : mostAndLeast(qrList, perQr);

        List<QrCode> activeQrs = qrList.stream()
                .filter(q -> q.getStatus() == QrStatus.active)
                .toList();
        double activeWithScans = activeQrs.isEmpty() ? 0 :
                100.0 * activeQrs.stream().filter(q -> perQr.getOrDefault(q.getId(), 0L) > 0).count()
                        / activeQrs.size();

        double dayCount = Math.max(1, window.toDays() + 1);

        return new CampaignMetricsResponse(
                events.size(), human.size(), bots, unique,
                (double) human.size() / dayCount,
                periodFrom, periodTo, prevEvents.size(), most, activeWithScans,
                groupBy(human, ScanEvent::getCountryCode),
                groupBy(human, ScanEvent::getDeviceType));
    }

    private CampaignMetricsResponse.MostScanned mostAndLeast(List<QrCode> qrList,
                                                             Map<UUID, Long> perQr) {
        QrCode most = qrList.stream()
                .max(Comparator.comparingLong(q -> perQr.getOrDefault(q.getId(), 0L)))
                .orElseThrow();
        QrCode least = qrList.stream()
                .min(Comparator.comparingLong(q -> perQr.getOrDefault(q.getId(), 0L)))
                .orElseThrow();
        return new CampaignMetricsResponse.MostScanned(
                most.getName(), perQr.getOrDefault(most.getId(), 0L),
                least.getName(), perQr.getOrDefault(least.getId(), 0L));
    }

    /**
     * Conteo de escaneos humanos (acumulado historico, sin periodo) por lotes para las
     * listas del frontend. Solo cuenta QRs propios y no borrados: los ids ajenos o
     * inexistentes se omiten (el frontend mostrara 0).
     */
    public Map<UUID, Long> humanScanCounts(UUID ownerId, List<UUID> ids) {
        List<UUID> owned = qrCodes.findByIdIn(ids).stream()
                .filter(q -> q.getDeletedAt() == null && q.getOwnerId().equals(ownerId))
                .map(QrCode::getId)
                .toList();
        if (owned.isEmpty()) {
            return Map.of();
        }

        Map<UUID, Long> counts = new HashMap<>();
        owned.forEach(id -> counts.put(id, 0L));
        for (ScanEvent event : scanEvents.findByQrCodeIdIn(owned)) {
            if (!event.isIsBot()) {
                counts.merge(event.getQrCodeId(), 1L, Long::sum);
            }
        }
        return counts;
    }

    private Map<String, Long> groupBy(List<ScanEvent> events,
                                      java.util.function.Function<ScanEvent, String> keyFn) {
        Map<String, Long> out = new HashMap<>();
        for (ScanEvent e : events) {
            String k = keyFn.apply(e);
            if (k != null) {
                out.merge(k, 1L, Long::sum);
            }
        }
        return out;
    }
}
