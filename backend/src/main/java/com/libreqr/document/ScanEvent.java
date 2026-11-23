package com.libreqr.document;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.util.UUID;

/**
 * Evento de escaneo de un QR dinamico.
 *
 * <p>Coleccion {@code scan_events}. Se genera en cada acceso a la direccion dinamica antes de
 * redirigir (REQ-MET-01). Los QR estaticos no generan eventos.</p>
 *
 * <p>{@code visitorHash} es un HMAC-SHA-256 de la cookie de primera parte, nunca el valor
 * original; los bots se registran por separado con {@code isBot} y no cuentan para los
 * visitantes unicos.</p>
 */
@Document(collection = "scan_events")
public class ScanEvent {

    @Id
    private UUID id;
    @Indexed
    private UUID qrCodeId;
    @Indexed
    private Instant scannedAt;
    private String visitorHash;
    private String countryCode;
    private String region;
    private String city;
    private String deviceType;
    private String operatingSystem;
    private String browser;
    private boolean isBot;
    private Instant createdAt;

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public UUID getQrCodeId() {
        return qrCodeId;
    }

    public void setQrCodeId(UUID qrCodeId) {
        this.qrCodeId = qrCodeId;
    }

    public Instant getScannedAt() {
        return scannedAt;
    }

    public void setScannedAt(Instant scannedAt) {
        this.scannedAt = scannedAt;
    }

    public String getVisitorHash() {
        return visitorHash;
    }

    public void setVisitorHash(String visitorHash) {
        this.visitorHash = visitorHash;
    }

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getDeviceType() {
        return deviceType;
    }

    public void setDeviceType(String deviceType) {
        this.deviceType = deviceType;
    }

    public String getOperatingSystem() {
        return operatingSystem;
    }

    public void setOperatingSystem(String operatingSystem) {
        this.operatingSystem = operatingSystem;
    }

    public String getBrowser() {
        return browser;
    }

    public void setBrowser(String browser) {
        this.browser = browser;
    }

    public boolean isIsBot() {
        return isBot;
    }

    public void setIsBot(boolean isBot) {
        this.isBot = isBot;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Instant createdAt) {
        this.createdAt = createdAt;
    }
}
