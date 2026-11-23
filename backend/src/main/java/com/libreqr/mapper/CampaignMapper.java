package com.libreqr.mapper;

import com.libreqr.document.Campaign;
import com.libreqr.dto.CampaignResponse;
import org.springframework.stereotype.Component;

/**
 * Conversion Campaign (documento) -> CampaignResponse (DTO).
 */
@Component
public class CampaignMapper {

    public CampaignResponse toResponse(Campaign campaign) {
        return new CampaignResponse(
                campaign.getId(),
                campaign.getName(),
                campaign.getDescription(),
                campaign.getStatus(),
                campaign.getCreatedAt(),
                campaign.getUpdatedAt());
    }
}
