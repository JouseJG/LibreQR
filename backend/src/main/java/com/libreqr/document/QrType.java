package com.libreqr.document;

/**
 * Tipo de codigo QR soportado en la primera version.
 *
 * <p>Equivalente a {@code enum qr_type} del diagrama (REQ-QR-01).</p>
 */
public enum QrType {
    url,
    text,
    email,
    wifi
}
