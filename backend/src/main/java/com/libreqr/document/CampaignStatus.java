package com.libreqr.document;

/**
 * Estado de una campana.
 *
 * <p>Las campanas nacen {@code active}; puede pausarse y reactivarse, o finalizar
 * (estado terminal desde el punto de vista de negocio, reactivable si hace falta).</p>
 */
public enum CampaignStatus {
    active,
    paused,
    finished
}
