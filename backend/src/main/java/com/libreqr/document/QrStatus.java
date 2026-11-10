package com.libreqr.document;

/**
 * Estado de un codigo QR.
 *
 * <p>Equivalente a {@code enum qr_status} del diagrama. Un QR {@code inactive} no debe
 * redirigir a su destino (REQ-QR-09).</p>
 */
public enum QrStatus {
    active,
    inactive
}
