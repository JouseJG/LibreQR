package com.libreqr.controller;

import com.libreqr.document.QrCode;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.service.RedirectService;
import com.libreqr.util.VisitorKeyService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

/**
 * Redirección de QRs dinámicos (REQ-QR-07, REQ-QR-09, REQ-MET-01).
 *
 * <p>{@code GET /r/{slug}} registra el escaneo antes de redirigir. Asigna (o reutiliza) una
 * cookie de primera parte con un identificador aleatorio; en el evento se guarda su
 * HMAC-SHA-256, nunca el valor original. Si el QR está desactivado se muestra una página
 * indicando que ya no está operativo (REQ-QR-09).</p>
 */
@RestController
public class RedirectController {

    private static final String VISITOR_COOKIE = "libreqr_vid";

    private static final org.springframework.http.MediaType TEXT_HTML_UTF8 =
            new org.springframework.http.MediaType(
                    org.springframework.http.MediaType.TEXT_HTML,
                    java.nio.charset.StandardCharsets.UTF_8);
    private static final String INACTIVE_PAGE = """
            <html lang="es"><head><title>QR no disponible</title></head>
            <body><h1>Este código QR ya no está operativo</h1>
            <p>El código ha sido desactivado por su propietario.</p></body></html>""";

    private final RedirectService redirects;
    private final VisitorKeyService visitorKeys;

    public RedirectController(RedirectService redirects, VisitorKeyService visitorKeys) {
        this.redirects = redirects;
        this.visitorKeys = visitorKeys;
    }

    @GetMapping("/r/{slug}")
    public ResponseEntity<String> redirect(
            @PathVariable String slug,
            HttpServletRequest request,
            HttpServletResponse response) {

        QrCode qr = redirects.resolve(slug); // 404 si no existe o no es dinámico

        // Cookie de visitante de primera parte (estimación de visitantes únicos).
        String visitorId = readVisitorCookie(request);
        if (visitorId == null) {
            visitorId = visitorKeys.newVisitorId();
            ResponseCookie cookie = ResponseCookie.from(VISITOR_COOKIE, visitorId)
                    .httpOnly(true)
                    .sameSite("Lax")
                    .path("/")
                    .maxAge(java.time.Duration.ofDays(398)) // <400 días (política de navegador)
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        }

        redirects.recordScan(qr, request, visitorKeys.hash(visitorId));

        if (!redirects.isActive(qr)) {
            return ResponseEntity.status(HttpStatus.GONE)
                    .contentType(TEXT_HTML_UTF8)
                    .body(INACTIVE_PAGE);
        }

        return ResponseEntity.status(HttpStatus.FOUND)
                .location(URI.create(qr.getPayload()))
                .build();
    }

    private String readVisitorCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (VISITOR_COOKIE.equals(c.getName()) && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}
