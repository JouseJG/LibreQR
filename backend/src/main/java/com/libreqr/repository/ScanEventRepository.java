package com.libreqr.repository;

import com.libreqr.document.ScanEvent;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.UUID;

/**
 * Repositorio de eventos de escaneo.
 *
 * <p>Escritura de eventos y consultas paginadas por QR y periodo (para métricas).</p>
 */
@Repository
public interface ScanEventRepository extends MongoRepository<ScanEvent, UUID> {

    Page<ScanEvent> findByQrCodeIdAndScannedAtBetween(
            UUID qrCodeId, Instant from, Instant to, Pageable pageable);

    long countByQrCodeId(UUID qrCodeId);

    long countByQrCodeIdAndIsBotFalse(UUID qrCodeId);
}
