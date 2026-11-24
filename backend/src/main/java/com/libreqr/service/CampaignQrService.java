package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignStatus;
import com.libreqr.document.CampaignQr;
import com.libreqr.document.QrCode;
import com.libreqr.dto.CampaignQrResponse;
import com.libreqr.exception.ConflictException;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import org.springframework.stereotype.Service;

import com.libreqr.dto.QrResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Asociacion campana-QR (REQ-CAM-02, REQ-CAM-05).
 *
 * <p>Anadir QRs existentes a una campana propia y quitarlos. Quitar un QR de la campana no
 * elimina el QR (REQ-CAM-05): queda desasignado. Un QR solo puede pertenecer a una campana
 * y esta debe estar activa.</p>
 */
@Service
public class CampaignQrService {

    private final CampaignQrRepository links;
    private final QrCodeRepository qrCodes;
    private final CampaignRepository campaigns;
    private final QrService qrQuery;

    public CampaignQrService(CampaignQrRepository links, QrCodeRepository qrCodes,
                             CampaignRepository campaigns, QrService qrQuery) {
        this.links = links;
        this.qrCodes = qrCodes;
        this.campaigns = campaigns;
        this.qrQuery = qrQuery;
    }

    public CampaignQrResponse add(UUID ownerId, UUID campaignId, UUID qrCodeId) {
        QrCode qr = qrCodes.findById(qrCodeId)
                .filter(q -> q.getDeletedAt() == null && q.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("QR no encontrado: " + qrCodeId));

        Campaign campaign = campaigns.findById(campaignId)
                .filter(c -> c.getDeletedAt() == null && c.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("Campana no encontrada: " + campaignId));
        if (campaign.getStatus() != CampaignStatus.active) {
            throw new ConflictException("La campana no esta activa");
        }
        if (qr.getCampaignId() != null && !qr.getCampaignId().equals(campaignId)) {
            throw new ConflictException("El QR ya pertenece a otra campana");
        }

        if (links.existsByCampaignIdAndQrCodeId(campaignId, qr.getId())) {
            throw new ConflictException("El QR ya esta en esta campana");
        }

        CampaignQr saved = links.save(CampaignQr.of(campaignId, qr.getId(), Instant.now()));

        if (qr.getCampaignId() == null) {
            // campanas historicas de QRs antiguos: fijamos la asignacion en el QR
            qr.setCampaignId(campaignId);
            qr.setUpdatedAt(Instant.now());
            qrCodes.save(qr);
        }
        return toResponse(saved);
    }

    /** Detalle de los QRs asociados: paginado, ordenable y buscable (perfil de campana). */
    public Page<QrResponse> details(UUID ownerId, UUID campaignId, Pageable pageable,
                                    String search) {
        List<UUID> qrIds = links.findByCampaignId(campaignId).stream()
                .map(CampaignQr::getQrCodeId)
                .toList();
        return qrQuery.listByIds(ownerId, qrIds, pageable, search);
    }

    public List<CampaignQrResponse> list(UUID campaignId) {
        return links.findByCampaignId(campaignId).stream().map(this::toResponse).toList();
    }

    public void remove(UUID campaignId, UUID qrCodeId) {
        if (!links.existsByCampaignIdAndQrCodeId(campaignId, qrCodeId)) {
            throw new ResourceNotFoundException("El QR no esta en esta campana: " + qrCodeId);
        }
        links.deleteByCampaignIdAndQrCodeId(campaignId, qrCodeId);

        // el QR sobrevive pero queda desasignado (REQ-CAM-05)
        qrCodes.findById(qrCodeId).ifPresent(qr -> {
            if (campaignId.equals(qr.getCampaignId())) {
                qr.setCampaignId(null);
                qr.setUpdatedAt(Instant.now());
                qrCodes.save(qr);
            }
        });
    }

    private CampaignQrResponse toResponse(CampaignQr link) {
        return new CampaignQrResponse(
                link.getCampaignId(), link.getQrCodeId(), link.getAddedAt());
    }
}
