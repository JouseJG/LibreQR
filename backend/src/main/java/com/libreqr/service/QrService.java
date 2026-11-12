package com.libreqr.service;

import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;
import com.libreqr.dto.QrResponse;
import com.libreqr.dto.QrUpsertRequest;
import com.libreqr.exception.InvalidContentException;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.mapper.QrMapper;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.util.SlugGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.UUID;

/**
 * Logica de negocio de los QRs (REQ-QR-*).
 *
 * <p>Valida el contenido por tipo (REQ-QR-02), gestiona el ciclo de vida y delega la
 * generacion de imagen a MinIO (pendiente de implementar; por ahora la clave de
 * almacenamiento queda sin fijar). El ownerId se recibira como parametro hasta que
 * exista autenticacion (Fase 2).</p>
 */
@Service
public class QrService {

    private final QrCodeRepository qrCodes;
    private final QrMapper mapper;
    private final SlugGenerator slugs;

    public QrService(QrCodeRepository qrCodes, QrMapper mapper, SlugGenerator slugs) {
        this.qrCodes = qrCodes;
        this.mapper = mapper;
        this.slugs = slugs;
    }

    public QrResponse create(UUID ownerId, QrUpsertRequest request) {
        validatePayload(request.type(), request.payload());

        Instant now = Instant.now();
        QrCode qr = new QrCode();
        qr.setId(UUID.randomUUID());
        qr.setOwnerId(ownerId);
        qr.setType(request.type());
        qr.setStatus(QrStatus.active);
        qr.setName(request.name());
        qr.setDescription(request.description());
        qr.setSlug(slugs.generate());
        qr.setPayload(request.payload());
        qr.setDesignConfig(request.designConfig());
        qr.setCreatedAt(now);
        qr.setUpdatedAt(now);
        return mapper.toResponse(qrCodes.save(qr));
    }

    public Page<QrResponse> list(UUID ownerId, Pageable pageable) {
        return qrCodes.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable).map(mapper::toResponse);
    }

    public QrResponse get(UUID ownerId, UUID id) {
        return mapper.toResponse(loadOwned(ownerId, id));
    }

    /**
     * Edicion: en QRs url cambia el destino sin tocar el slug (REQ-QR-07); en estaticos
     * el payload nuevo requiere volver a descargar la imagen (REQ-QR-08).
     */
    public QrResponse update(UUID ownerId, UUID id, QrUpsertRequest request) {
        validatePayload(request.type(), request.payload());

        QrCode qr = loadOwned(ownerId, id);
        qr.setType(request.type());
        qr.setName(request.name());
        qr.setDescription(request.description());
        qr.setPayload(request.payload());
        qr.setDesignConfig(request.designConfig());
        qr.setUpdatedAt(Instant.now());
        return mapper.toResponse(qrCodes.save(qr));
    }

    /** Desactivar QR: deja de redirigir pero no se elimina (REQ-QR-09). */
    public QrResponse deactivate(UUID ownerId, UUID id) {
        QrCode qr = loadOwned(ownerId, id);
        qr.setStatus(QrStatus.inactive);
        qr.setUpdatedAt(Instant.now());
        return mapper.toResponse(qrCodes.save(qr));
    }

    /** Eliminar QR con borrado logico (REQ-QR-10). */
    public void delete(UUID ownerId, UUID id) {
        QrCode qr = loadOwned(ownerId, id);
        qr.setDeletedAt(Instant.now());
        qr.setUpdatedAt(Instant.now());
        qrCodes.save(qr);
    }

    private QrCode loadOwned(UUID ownerId, UUID id) {
        QrCode qr = qrCodes.findById(id)
                .filter(q -> q.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("QR no encontrado: " + id));
        // Control de acceso en backend: ocultar opciones en la UI no es suficiente.
        if (!qr.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("QR no encontrado: " + id);
        }
        return qr;
    }

    private void validatePayload(QrType type, String payload) {
        switch (type) {
            case url -> {
                try {
                    URI uri = new URI(payload);
                    if (!"http".equals(uri.getScheme()) && !"https".equals(uri.getScheme())) {
                        throw new InvalidContentException(
                                "El enlace debe empezar por http:// o https://");
                    }
                } catch (URISyntaxException ex) {
                    throw new InvalidContentException("El enlace no tiene un formato valido");
                }
            }
            case email -> {
                if (!payload.matches("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$")) {
                    throw new InvalidContentException("El correo no tiene un formato valido");
                }
            }
            case text -> {
                // Solo presencia, ya validada por @NotBlank.
            }
            case wifi -> {
                // SSID obligatorio; el resto del formato (password/seguridad) se define
                // al implementar la codificacion WiFi.
                if (payload.isBlank()) {
                    throw new InvalidContentException("El nombre de red (SSID) es obligatorio");
                }
            }
        }
    }
}
