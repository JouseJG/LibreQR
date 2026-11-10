package com.libreqr.document;

import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Relacion N:M entre campanas y QRs.
 *
 * <p>Coleccion {@code campaign_qrs}. La clave compuesta (campaignId, qrCodeId) identifica el
 * documento. Quitar un QR de una campana no elimina el QR (REQ-CAM-05).</p>
 */
@Document(collection = "campaign_qrs")
public class CampaignQr {

    private UUID campaignId;
    private UUID qrCodeId;
    private Instant addedAt;

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public UUID getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(UUID qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public Instant getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(Instant addedAt) {
        this.addedAt = addedAt;
    }
}
