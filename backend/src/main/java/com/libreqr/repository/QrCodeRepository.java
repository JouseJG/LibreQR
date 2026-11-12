package com.libreqr.repository;

import com.libreqr.document.QrCode;
import com.libreqr.document.QrType;
import com.libreqr.document.QrStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de códigos QR.
 *
 * <p>CRUD + paginación y búsquedas por owner, tipo, estado y slug (para redirecciones).</p>
 */
@Repository
public interface QrCodeRepository extends MongoRepository<QrCode, UUID> {

    Page<QrCode> findByOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);

    Page<QrCode> findByOwnerIdAndTypeAndDeletedAtIsNull(UUID ownerId, QrType type, Pageable pageable);

    Page<QrCode> findByOwnerIdAndStatusAndDeletedAtIsNull(UUID ownerId, QrStatus status, Pageable pageable);

    QrCode findBySlugAndDeletedAtIsNull(String slug);

    List<QrCode> findByOwnerIdAndDeletedAtIsNull(UUID ownerId);
}
