package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Clave de API de un usuario (decisi\u00f3n 2026-11-13).
 *
 * <p>El frontend puede llamar con {@code Authorization: Bearer <clave>}. Solo se almacena
 * el <strong>hash SHA-256</strong> de la clave; el valor en claro se muestra una unica vez
 * al crearla. {@code display} guarda un prefijo para poder reconocerla en la UI.</p>
 */
@Document(collection = "api_keys")
public class ApiKey {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String keyHash;

    private String display;

    @Indexed
    private UUID userId;

    private String name;
    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastUsedAt;
    private boolean revoked;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getKeyHash() {
        return keyHash;
    }

    public void setKeyHash(String keyHash) {
        this.keyHash = keyHash;
    }

    public String getDisplay() {
        return display;
    }

    public void setDisplay(String display) {
        this.display = display;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public void setExpiresAt(Instant expiresAt) {
        this.expiresAt = expiresAt;
    }

    public Instant getLastUsedAt() {
        return lastUsedAt;
    }

    public void setLastUsedAt(Instant lastUsedAt) {
        this.lastUsedAt = lastUsedAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
