package com.libreqr.dto;

import java.time.LocalDate;

/** Punto diario de la serie de escaneos del dashboard (escaneos humanos). */
public record DailyScanStat(LocalDate date, long scans) {
}
