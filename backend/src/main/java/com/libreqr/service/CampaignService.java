package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignStatus;
import com.libreqr.document.CampaignQr;
import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.dto.CampaignResponse;
import com.libreqr.dto.CampaignUpsertRequest;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.mapper.CampaignMapper;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Logica de negocio de las campanas (REQ-CAM-01..05).
 *
 * <p>CRUD paginado + borrado logico. La asociacion de QRs (REQ-CAM-02) se gestiona en
 * CampaignQrService; las metricas agregadas (REQ-CAM-04) se anadiran con la fase de
 * metricas.</p>
 */
@Service
public class CampaignService {

    private final CampaignRepository campaigns;
    private final CampaignMapper mapper;
    private final MongoTemplate mongo;
    private final CampaignQrRepository campaignLinks;
    private final QrCodeRepository qrCodes;

    public CampaignService(CampaignRepository campaigns, CampaignMapper mapper,
                           MongoTemplate mongo, CampaignQrRepository campaignLinks,
                           QrCodeRepository qrCodes) {
        this.campaigns = campaigns;
        this.mapper = mapper;
        this.mongo = mongo;
        this.campaignLinks = campaignLinks;
        this.qrCodes = qrCodes;
    }

    public CampaignResponse create(UUID ownerId, CampaignUpsertRequest request) {
        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setOwnerId(ownerId);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setStatus(CampaignStatus.active);
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        return mapper.toResponse(campaigns.save(campaign));
    }

    /** Listado paginado con busqueda y estado opcionales. */
    public Page<CampaignResponse> list(UUID ownerId, Pageable pageable, String search,
                                       CampaignStatus status) {
        Criteria base = Criteria.where("ownerId").is(ownerId).and("deletedAt").isNull();
        List<Criteria> operators = new java.util.ArrayList<>();
        if (status != null) {
            if (status == CampaignStatus.active) {
                // campanas creadas antes del campo status se consideran activas
                operators.add(new Criteria().orOperator(
                        Criteria.where("status").is(CampaignStatus.active),
                        Criteria.where("status").is(null)));
            } else {
                base = base.and("status").is(status);
            }
        }
        if (search != null && !search.isBlank()) {
            String quoted = Pattern.quote(search.trim());
            operators.add(new Criteria().orOperator(
                    Criteria.where("name").regex(quoted, "i"),
                    Criteria.where("description").regex(quoted, "i")));
        }
        if (!operators.isEmpty()) {
            base = base.andOperator(operators.toArray(Criteria[]::new));
        }

        long total = mongo.count(new Query(base), Campaign.class);
        List<Campaign> content = mongo.find(new Query(base).with(pageable), Campaign.class);
        return new PageImpl<>(content.stream().map(mapper::toResponse).toList(), pageable,
                total);
    }

    /**
     * Pausar/reactivar/finalizar campana.
     *
     * <p>Cascado simetrico sobre los QRs vinculados (REQ-QR-09 + REQ-CAM-02): al pausar o
     * finalizar, los QRs quedan inactivos (no deben redirigir desde una campana no activa);
     * al reactivar la campana, los QRs vuelven a activarse.</p>
     */
    public CampaignResponse changeStatus(UUID ownerId, UUID id, CampaignStatus status) {
        Campaign campaign = loadOwned(ownerId, id);
        campaign.setStatus(status);
        campaign.setUpdatedAt(Instant.now());
        Campaign saved = campaigns.save(campaign);

        List<UUID> qrIds = campaignLinks.findByCampaignId(id).stream()
                .map(CampaignQr::getQrCodeId)
                .toList();
        if (!qrIds.isEmpty()) {
            QrStatus target = status == CampaignStatus.active
                    ? QrStatus.active : QrStatus.inactive;
            Instant now = Instant.now();
            List<QrCode> toCascade = qrCodes.findByIdIn(qrIds).stream()
                    .filter(q -> q.getDeletedAt() == null && q.getStatus() != target)
                    .peek(q -> {
                        q.setStatus(target);
                        q.setUpdatedAt(now);
                    })
                    .toList();
            if (!toCascade.isEmpty()) {
                qrCodes.saveAll(toCascade);
            }
        }
        return mapper.toResponse(saved);
    }

    public CampaignResponse get(UUID ownerId, UUID id) {
        return mapper.toResponse(loadOwned(ownerId, id));
    }

    public CampaignResponse update(UUID ownerId, UUID id, CampaignUpsertRequest request) {
        Campaign campaign = loadOwned(ownerId, id);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setUpdatedAt(Instant.now());
        return mapper.toResponse(campaigns.save(campaign));
    }

    /** Borrado logico: la campana deja de verse pero no se eliminan sus QRs. */
    public void delete(UUID ownerId, UUID id) {
        Campaign campaign = loadOwned(ownerId, id);
        campaign.setDeletedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaigns.save(campaign);
    }

    /**
     * Carga la campana verificando que es del usuario. Si no existe o es de otro, 404
     * (no se filtra la existencia del recurso).
     */
    Campaign loadOwned(UUID ownerId, UUID id) {
        Campaign campaign = campaigns.findById(id)
                .filter(c -> c.getDeletedAt() == null)
                .orElseThrow(() -> new ResourceNotFoundException("Campana no encontrada: " + id));
        if (!campaign.getOwnerId().equals(ownerId)) {
            throw new ResourceNotFoundException("Campana no encontrada: " + id);
        }
        return campaign;
    }
}
