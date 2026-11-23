package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

/**
 * Codigo QR.
 *
 * <p>Coleccion {@code qr_codes}. Los QR dinamicos (tipo {@code url}) usan {@code slug} como
 * direccion de redireccion medible; los estaticos (text, email, wifi) no generan eventos de
 * escaneo. {@code designConfig} guarda el diseno personalizable (marco, forma, logo) y
 * {@code imageUrl} es la clave de almacenamiento en MinIO, no una URL publica.</p>
 */
@Document(collection = "qr_codes")
public class QrCode {

    @Id
    private UUID id;
    private UUID ownerId;
    private QrType type;
    private QrStatus status;
    private String name;
    private String description;
    private String slug;
    private String payload;
    private UUID campaignId;
    private Map<String, Object> designConfig;
    private String imageUrl;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
    }

    public QrType getType() {
        return type;
    }

    public void setType(QrType type) {
        this.type = type;
    }

    public QrStatus getStatus() {
        return status;
    }

    public void setStatus(QrStatus status) {
        this.status = status;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getSlug() {
        return slug;
    }

    public void setSlug(String slug) {
        this.slug = slug;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String payload) {
        this.payload = payload;
    }

    public UUID getCampaignId() {
        return campaignId;
    }

    public void setCampaignId(UUID campaignId) {
        this.campaignId = campaignId;
    }

    public Map<String, Object> getDesignConfig() {
        return designConfig;
    }

    public void setDesignConfig(Map<String, Object> designConfig) {
        this.designConfig = designConfig;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Instant updatedAt) {
        this.updatedAt = updatedAt;
    }

    public Instant getDeletedAt() {
        return deletedAt;
    }

    public void setDeletedAt(Instant deletedAt) {
        this.deletedAt = deletedAt;
    }
}
