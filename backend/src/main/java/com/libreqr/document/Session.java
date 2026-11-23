package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Sesion de usuario (modelo cookie-servidor).
 *
 * <p>El navegador recibe una cookie <em>solo</em> con el identificador aleatorio de la
 * sesion; aqui se guarda el <strong>hash SHA-256</strong> del token de sesion, nunca el
 * valor en claro (mismo criterio que con las claves de visitante). La propiedad de los
 * recursos se deriva de esta sesion, no de un id que mande el cliente.</p>
 */
@Document(collection = "sessions")
public class Session {

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String tokenHash;

    @Indexed
    private UUID userId;

    private Instant createdAt;
    private Instant expiresAt;
    private Instant lastSeenAt;
    private boolean revoked;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public void setTokenHash(String tokenHash) {
        this.tokenHash = tokenHash;
    }

    public UUID getUserId() {
        return userId;
    }

    public void setUserId(UUID userId) {
        this.userId = userId;
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

    public Instant getLastSeenAt() {
        return lastSeenAt;
    }

    public void setLastSeenAt(Instant lastSeenAt) {
        this.lastSeenAt = lastSeenAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
