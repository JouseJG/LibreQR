package com.libreqr.service;

import com.libreqr.document.CampaignQr;
import com.libreqr.document.QrCode;
import com.libreqr.dto.CampaignQrResponse;
import com.libreqr.exception.ConflictException;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.QrCodeRepository;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Asociacion campana-QR (REQ-CAM-02, REQ-CAM-05).
 *
 * <p>Anadir QRs existentes a una campana propia y quitarlos. Quitar un QR de la campana no
 * elimina el QR (REQ-CAM-05). Solo se pueden anadir QRs propios y vivos.</p>
 */
@Service
public class CampaignQrService {

    private final CampaignQrRepository links;
    private final QrCodeRepository qrCodes;

    public CampaignQrService(CampaignQrRepository links, QrCodeRepository qrCodes) {
        this.links = links;
        this.qrCodes = qrCodes;
    }

    public CampaignQrResponse add(UUID ownerId, UUID campaignId, UUID qrCodeId) {
        QrCode qr = qrCodes.findById(qrCodeId)
                .filter(q -> q.getDeletedAt() == null && q.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException("QR no encontrado: " + qrCodeId));

        if (links.existsByCampaignIdAndQrCodeId(campaignId, qr.getId())) {
            throw new ConflictException("El QR ya esta en esta campana");
        }

        CampaignQr saved = links.save(CampaignQr.of(campaignId, qr.getId(), Instant.now()));
        return toResponse(saved);
    }

    public List<CampaignQrResponse> list(UUID campaignId) {
        return links.findByCampaignId(campaignId).stream().map(this::toResponse).toList();
    }

    public void remove(UUID campaignId, UUID qrCodeId) {
        if (!links.existsByCampaignIdAndQrCodeId(campaignId, qrCodeId)) {
            throw new ResourceNotFoundException("El QR no esta en esta campana: " + qrCodeId);
        }
        links.deleteByCampaignIdAndQrCodeId(campaignId, qrCodeId);
    }

    private CampaignQrResponse toResponse(CampaignQr link) {
        return new CampaignQrResponse(
                link.getCampaignId(), link.getQrCodeId(), link.getAddedAt());
    }
}
