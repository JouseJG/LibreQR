package com.libreqr.document;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Usuario de LibreQR.
 *
 * <p>{@code passwordHash} es un digest SHA-256 con salt aleatoria (mejorable a bcrypt/argon2;
 * anotado en el plan). {@code role}: USER o ADMIN; los permisos se verifican en el backend.
 * La coleccion {@code users} la comparten el antiguo modelo (sin usar) y esta cuenta: el
 * modelo anterior se retira cuando el frontend consuma la API de auth.</p>
 */
@Document(collection = "users")
@JsonIgnoreProperties(ignoreUnknown = true)
public class UserAccount {

    public enum Role { USER, ADMIN }

    @Id
    private UUID id;

    @Indexed(unique = true)
    private String email;

    private String name;
    private String passwordHash;
    private String passwordSalt;
    private Role role = Role.USER;
    private Instant createdAt;
    private Instant updatedAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public String getPasswordSalt() {
        return passwordSalt;
    }

    public void setPasswordSalt(String passwordSalt) {
        this.passwordSalt = passwordSalt;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
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
}
