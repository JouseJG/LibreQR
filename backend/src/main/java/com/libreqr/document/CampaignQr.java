package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.domain.AbstractAggregateRoot;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Relacion N:M entre campanas y QRs.
 *
 * <p>Coleccion {@code campaign_qrs}. La clave primaria es compuesta (campaignId, qrCodeId);
 * en MongoDB se modela como un id de texto {@code campaignId_qrCodeId} (los dos UUIDs se
 * guardan aparte como claves foraneas). El id se deriva al construir la relacion con
 * {@link #of}. Quitar un QR de una campana no elimina el QR (REQ-CAM-05).</p>
 */
@Document(collection = "campaign_qrs")
public class CampaignQr extends AbstractAggregateRoot<CampaignQr> {

    @Id
    private String id;
    private UUID campaignId;
    private UUID qrCodeId;
    private Instant addedAt;

    /**
     * Fabrica la relacion calculando el id compuesto {@code campaignId_qrCodeId}.
     */
    public static CampaignQr of(UUID campaignId, UUID qrCodeId, Instant addedAt) {
        CampaignQr link = new CampaignQr();
        link.campaignId = campaignId;
        link.qrCodeId = qrCodeId;
        link.addedAt = addedAt;
        link.id = campaignId + "_" + qrCodeId;
        return link;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

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
