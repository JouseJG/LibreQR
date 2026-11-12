package com.libreqr.repository;

import com.libreqr.document.Campaign;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de campañas.
 *
 * <p>CRUD + paginación para listar campañas propias (sin borrado lógico).</p>
 */
@Repository
public interface CampaignRepository extends MongoRepository<Campaign, UUID> {

    Page<Campaign> findByOwnerIdAndDeletedAtIsNull(UUID ownerId, Pageable pageable);

    List<Campaign> findByOwnerIdAndDeletedAtIsNull(UUID ownerId);
}
