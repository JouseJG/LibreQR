package com.libreqr.repository;

import com.libreqr.document.ScanEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Repositorio de eventos de escaneo.
 *
 * <p>Escritura de eventos y consultas basicas por QR y periodo. Las agregaciones de
 * metricas (grupaciones, uniques, comparativa de periodos) se hacen con MongoTemplate en
 * MetricsService, no aqui.</p>
 */
@Repository
public interface ScanEventRepository extends MongoRepository<ScanEvent, UUID> {

    Page<ScanEvent> findByQrCodeIdAndScannedAtBetween(
            UUID qrCodeId, Instant from, Instant to, Pageable pageable);

    List<ScanEvent> findByQrCodeIdAndScannedAtBetween(
            UUID qrCodeId, Instant from, Instant to);

    List<ScanEvent> findByQrCodeIdInAndScannedAtBetween(
            List<UUID> qrCodeIds, Instant from, Instant to);

    List<ScanEvent> findByQrCodeIdIn(List<UUID> qrCodeIds);

    List<ScanEvent> findByQrCodeId(UUID qrCodeId);

    long countByQrCodeId(UUID qrCodeId);

    long countByQrCodeIdAndIsBotFalse(UUID qrCodeId);
}
