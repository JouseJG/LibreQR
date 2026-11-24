package com.libreqr.service;

import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;
import com.libreqr.document.ScanEvent;
import com.libreqr.exception.InvalidContentException;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.repository.ScanEventRepository;
import com.libreqr.util.ScanClassifier;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

/**
 * Redireccion y registro de escaneos de QRs dinamicos
 * (REQ-QR-09, REQ-MET-01, identificacion y registro de escaneos del documento).
 *
 * <p>Cada acceso a la direccion dinamica genera un evento ANTES de redirigir. El evento
 * clasifica la peticion como bot o no bot de forma aproximada (user-agent) y estima el
 * visitante con el hash de la cookie de primera parte (el hash lo calcula el controlador;
 * aqui solo se almacena, nunca el identificador original).</p>
 */
@Service
public class RedirectService {

    private final QrCodeRepository qrCodes;
    private final ScanEventRepository scanEvents;
    private final ScanClassifier classifier;

    public RedirectService(QrCodeRepository qrCodes,
                           ScanEventRepository scanEvents,
                           ScanClassifier classifier) {
        this.qrCodes = qrCodes;
        this.scanEvents = scanEvents;
        this.classifier = classifier;
    }

    /**
     * Resuelve el slug. La redireccion es anonima (no hay owner).
     */
    public QrCode resolve(String slug) {
        QrCode qr = qrCodes.findBySlugAndDeletedAtIsNull(slug);
        if (qr == null) {
            throw new ResourceNotFoundException("QR no encontrado: " + slug);
        }
        if (qr.getType() != QrType.url) {
            throw new InvalidContentException("Este tipo de QR no pasa por el servidor");
        }
        return qr;
    }

    /**
     * Registra el escaneo (REQ-MET-01). No altera el flujo: el controlador redirige despues.
     *
     * @param visitorKeyHash HMAC-SHA-256 de la cookie de visitante; null si no se pudo asignar
     */
    public void recordScan(QrCode qr, HttpServletRequest request, String visitorKeyHash) {
        String userAgent = request.getHeader("User-Agent");

        ScanEvent ev = new ScanEvent();
        ev.setId(UUID.randomUUID());
        ev.setQrCodeId(qr.getId());
        ev.setScannedAt(Instant.now());
        ev.setVisitorHash(visitorKeyHash);
        // Pais aproximado por Accept-Language (REQ-MET-07: sin GeoIP dedicado, no inventar).
        ev.setCountryCode(com.libreqr.util.ScanClassifier
                .countryFromAcceptLanguage(request.getHeader("Accept-Language")));
        ev.setRegion(null);
        ev.setCity(null);
        ev.setDeviceType(classifier.deviceType(userAgent));
        ev.setOperatingSystem(classifier.operatingSystem(userAgent));
        ev.setBrowser(classifier.browser(userAgent));
        ev.setIsBot(classifier.isBot(userAgent));
        ev.setCreatedAt(Instant.now());
        scanEvents.save(ev);
    }

    /** QR desactivado: se registra el escaneo y se muestra aviso, no redirige (REQ-QR-09). */
    public boolean isActive(QrCode qr) {
        return qr.getStatus() == QrStatus.active;
    }
}
