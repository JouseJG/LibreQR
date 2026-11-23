package com.libreqr.dto;

/**
 * Resumen global para el dashboard (una unica llamada).
 *
 * <p>Los contadores de escaneos y visitantes unicos son del mes natural en curso;
 * los campos {@code *PreviousMonth} cubren el mismo tramo del mes anterior para que
 * el cliente calcule la comparativa. QRs y campanas se contabilizan por estado y
 * totales (sin borrar).</p>
 */
public record StatsSummaryResponse(
        long scansCurrentMonth,
        long scansPreviousMonth,
        long uniqueCurrentMonth,
        long uniquePreviousMonth,
        long activeQrs,
        long totalQrs,
        long activeCampaigns,
        long totalCampaigns
) {
}
