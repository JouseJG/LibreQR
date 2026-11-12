package com.libreqr.service;

import com.libreqr.document.Campaign;
import com.libreqr.dto.CampaignResponse;
import com.libreqr.dto.CampaignUpsertRequest;
import com.libreqr.exception.ResourceNotFoundException;
import com.libreqr.mapper.CampaignMapper;
import com.libreqr.repository.CampaignRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.UUID;

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

    public CampaignService(CampaignRepository campaigns, CampaignMapper mapper) {
        this.campaigns = campaigns;
        this.mapper = mapper;
    }

    public CampaignResponse create(UUID ownerId, CampaignUpsertRequest request) {
        Instant now = Instant.now();
        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setOwnerId(ownerId);
        campaign.setName(request.name());
        campaign.setDescription(request.description());
        campaign.setCreatedAt(now);
        campaign.setUpdatedAt(now);
        return mapper.toResponse(campaigns.save(campaign));
    }

    public Page<CampaignResponse> list(UUID ownerId, Pageable pageable) {
        return campaigns.findByOwnerIdAndDeletedAtIsNull(ownerId, pageable)
                .map(mapper::toResponse);
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
