package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Campana que agrupa varios QRs.
 *
 * <p>Coleccion {@code campaigns}. El borrado es logico: {@code deletedAt} queda {@code null}
 * mientras la campana esta viva.</p>
 */
@Document(collection = "campaigns")
public class Campaign {

    @Id
    private UUID id;
    private UUID ownerId;
    private String name;
    private String description;
    private CampaignStatus status;
    private Instant createdAt;
    private Instant updatedAt;
    private Instant deletedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public CampaignStatus getStatus() {
        return status == null ? CampaignStatus.active : status;
    }

    public void setStatus(CampaignStatus status) {
        this.status = status;
    }

    public UUID getOwnerId() {
        return ownerId;
    }

    public void setOwnerId(UUID ownerId) {
        this.ownerId = ownerId;
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
