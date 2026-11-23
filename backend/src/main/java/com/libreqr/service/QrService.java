package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignStatus;
import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;
import com.libreqr.dto.QrResponse;
import com.libreqr.dto.QrUpsertRequest;
import com.libreqr.exception.ConflictException;
import com.libreqr.exception.InvalidContentException;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.mapper.QrMapper;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import com.libreqr.util.SlugGenerator;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URISyntaxException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

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
    private final MongoTemplate mongo;
    private final CampaignRepository campaigns;
    private final CampaignQrRepository campaignLinks;

    public QrService(QrCodeRepository qrCodes, QrMapper mapper, SlugGenerator slugs,
                     MongoTemplate mongo, CampaignRepository campaigns,
                     CampaignQrRepository campaignLinks) {
        this.qrCodes = qrCodes;
        this.mapper = mapper;
        this.slugs = slugs;
        this.mongo = mongo;
        this.campaigns = campaigns;
        this.campaignLinks = campaignLinks;
    }

    public QrResponse create(UUID ownerId, QrUpsertRequest request) {
        validatePayload(request.type(), request.payload());
        Campaign campaign = loadCampaign(ownerId, request.campaignId());
        if (campaign.getStatus() != CampaignStatus.active) {
            throw new ConflictException(
                    "La campana no esta activa; reactiva la campana antes de crear QRs");
        }

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
        qr.setCampaignId(campaign.getId());
        qr.setDesignConfig(request.designConfig());
        qr.setCreatedAt(now);
        qr.setUpdatedAt(now);
        QrCode saved = qrCodes.save(qr);

        // Espejo de la relacion (campaign_qrs) para las consultas de campana/metricas.
        if (!campaignLinks.existsByCampaignIdAndQrCodeId(campaign.getId(), saved.getId())) {
            campaignLinks.save(
                    com.libreqr.document.CampaignQr.of(
                            campaign.getId(), saved.getId(), now));
        }
        return mapper.toResponse(saved);
    }

    /**
     * Listado paginado con filtros opcionales de busqueda (nombre/descripcion, sin
     * distincion de mayusculas) y de estado. El filtrado se hace en el servidor (Mongo)
     * para que la paginacion siga siendo coherente con los resultados.
     */
    public Page<QrResponse> list(UUID ownerId, Pageable pageable, String search,
                                 QrStatus status) {
        Criteria base = Criteria.where("ownerId").is(ownerId).and("deletedAt").isNull();
        if (status != null) {
            base = base.and("status").is(status);
        }
        if (search != null && !search.isBlank()) {
            String quoted = Pattern.quote(search.trim());
            base = base.andOperator(new Criteria().orOperator(
                    Criteria.where("name").regex(quoted, "i"),
                    Criteria.where("description").regex(quoted, "i")));
        }

        Query countQuery = new Query(base);
        long total = mongo.count(countQuery, QrCode.class);

        Query pageQuery = new Query(base).with(pageable);
        List<QrCode> content = mongo.find(pageQuery, QrCode.class);
        return new PageImpl<>(content.stream().map(mapper::toResponse).toList(), pageable,
                total);
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
        Campaign campaign = loadCampaign(ownerId, request.campaignId());

        QrCode qr = loadOwned(ownerId, id);
        UUID previousCampaign = qr.getCampaignId();
        if (previousCampaign != null && !previousCampaign.equals(campaign.getId())
                && campaign.getStatus() != CampaignStatus.active) {
            throw new ConflictException(
                    "La campana de destino no esta activa");
        }

        qr.setType(request.type());
        qr.setName(request.name());
        qr.setDescription(request.description());
        qr.setPayload(request.payload());
        qr.setCampaignId(campaign.getId());
        qr.setDesignConfig(request.designConfig());
        qr.setUpdatedAt(Instant.now());
        QrCode saved = qrCodes.save(qr);

        if (previousCampaign != null && !previousCampaign.equals(campaign.getId())) {
            // movemos el espejo de la relacion a la campana nueva
            if (campaignLinks.existsByCampaignIdAndQrCodeId(previousCampaign, saved.getId())) {
                campaignLinks.deleteByCampaignIdAndQrCodeId(previousCampaign, saved.getId());
            }
            if (!campaignLinks.existsByCampaignIdAndQrCodeId(campaign.getId(), saved.getId())) {
                campaignLinks.save(com.libreqr.document.CampaignQr.of(
                        campaign.getId(), saved.getId(), Instant.now()));
            }
        }
        return mapper.toResponse(saved);
    }

    /** Desactivar QR: deja de redirigir pero no se elimina (REQ-QR-09). */
    public QrResponse deactivate(UUID ownerId, UUID id) {
        QrCode qr = loadOwned(ownerId, id);
        qr.setStatus(QrStatus.inactive);
        qr.setUpdatedAt(Instant.now());
        return mapper.toResponse(qrCodes.save(qr));
    }

    /**
     * Reactivar QR inactivo. Si su campana esta pausada o finalizada se rechaza (409):
     * un QR de una campana no activa no debe redirigir.
     */
    public QrResponse reactivate(UUID ownerId, UUID id) {
        QrCode qr = loadOwned(ownerId, id);
        if (qr.getStatus() == QrStatus.active) {
            return mapper.toResponse(qr);
        }
        if (qr.getCampaignId() != null) {
            Campaign campaign = campaigns.findById(qr.getCampaignId())
                    .filter(c -> c.getOwnerId().equals(ownerId) && c.getDeletedAt() == null)
                    .orElseThrow(() -> new ConflictException(
                            "La campana asociada ya no existe; asigna el QR a otra campana"));
            if (campaign.getStatus() != CampaignStatus.active) {
                throw new ConflictException(
                        "La campana asociada no esta activa; reactiva la campana primero");
            }
        }
        qr.setStatus(QrStatus.active);
        qr.setUpdatedAt(Instant.now());
        return mapper.toResponse(qrCodes.save(qr));
    }

    private Campaign loadCampaign(UUID ownerId, UUID campaignId) {
        return campaigns.findById(campaignId)
                .filter(c -> c.getDeletedAt() == null && c.getOwnerId().equals(ownerId))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Campana no encontrada: " + campaignId));
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
