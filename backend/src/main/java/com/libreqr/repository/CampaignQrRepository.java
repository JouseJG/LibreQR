package com.libreqr.repository;

import com.libreqr.document.CampaignQr;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

/**
 * Repositorio de la relación campaña-QR.
 *
 * <p>Añadir/quitar QRs de campañas y listar los QRs de una campaña.</p>
 */
@Repository
public interface CampaignQrRepository extends MongoRepository<CampaignQr, String> {

    List<CampaignQr> findByCampaignId(UUID campaignId);

    List<CampaignQr> findByQrCodeId(UUID qrCodeId);

    List<CampaignQr> findByCampaignIdIn(List<UUID> campaignIds);

    boolean existsByCampaignIdAndQrCodeId(UUID campaignId, UUID qrCodeId);

    void deleteByCampaignIdAndQrCodeId(UUID campaignId, UUID qrCodeId);
}
