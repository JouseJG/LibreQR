package com.libreqr.util;

import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Utilidades de medicion: deteccion aproximada de bots (criterio del documento) y
 * clasificacion de dispositivo/OS desde el user agent (REQ-MET-03).
 *
 * <p>Todas las clasificaciones son aproximadas: cuando no pueden determinarse devuelven
 * null; la interfaz no debe inventar valores (REQ-MET-07).</p>
 */
@Component
public class ScanClassifier {

    private static final String[] BOT_MARKERS = {
            "bot", "crawler", "spider", "curl", "wget", "python-requests",
            "headless", "phantomjs", "lighthouse", "scan", "http"
    };

    /** La clasificacion es aproximada; una peticion sin user-agent se marca como bot. */
    public boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) {
            return true;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        for (String marker : BOT_MARKERS) {
            if (ua.contains(marker)) {
                return true;
            }
        }
        return false;
    }

    public String deviceType(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("ipad") || ua.contains("tablet") || ua.contains("playbook")) {
            return "tablet";
        }
        if (ua.contains("mobi") || ua.contains("iphone") || ua.contains("android")) {
            return "mobile";
        }
        return "desktop";
    }

    public String operatingSystem(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("android")) {
            return "android";
        }
        if (ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios")) {
            return "ios";
        }
        if (ua.contains("win")) {
            return "windows";
        }
        if (ua.contains("mac")) {
            return "macos";
        }
        if (ua.contains("linux")) {
            return "linux";
        }
        return null;
    }

    /**
     * Pais aproximado a partir del subtag de region del Accept-Language (REQ-MET-07:
     * aproximacion honesta, sin servicio GeoIP dedicado; null si no se deduce).
     */
    public static String countryFromAcceptLanguage(String header) {
        if (header == null || header.isBlank()) {
            return null;
        }
        String first = header.split(",")[0].trim().split(";")[0].trim();
        String[] parts = first.split("[-_]");
        if (parts.length > 1 && parts[1].length() == 2) {
            return parts[1].toUpperCase(Locale.ROOT);
        }
        return switch (parts[0].toLowerCase(Locale.ROOT)) {
            case "es", "ca", "gl" -> "ES";
            case "pt" -> "PT";
            case "fr" -> "FR";
            case "de" -> "DE";
            case "it" -> "IT";
            case "en" -> "GB";
            default -> null;
        };
    }

    /** Dia UTC en formato yyyy-MM-dd (para agrupar por dia, REQ-MET-06). */
    public static String dayKey(Instant instant) {
        return DateTimeFormatter.ofPattern("yyyy-MM-dd")
                .withZone(ZoneOffset.UTC)
                .format(instant);
    }

    /** Hora UTC (0-23). */
    public static int hourUtc(Instant instant) {
        return instant.atZone(ZoneOffset.UTC).getHour();
    }

    /** Deteccion aproximada de navegador (sin librerias externas, criterio simple). */
    public String browser(String userAgent) {
        if (userAgent == null) {
            return null;
        }
        String ua = userAgent.toLowerCase(Locale.ROOT);
        if (ua.contains("edg")) return "edge";
        if (ua.contains("chrome") || ua.contains("crios")) return "chrome";
        if (ua.contains("firefox") || ua.contains("fxios")) return "firefox";
        if (ua.contains("safari")) return "safari";
        if (ua.contains("opera") || ua.contains("opr")) return "opera";
        return null;
    }
}
