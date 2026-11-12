package com.libreqr;

import com.libreqr.document.Campaign;
import com.libreqr.document.CampaignQr;
import com.libreqr.document.QrCode;
import com.libreqr.document.QrStatus;
import com.libreqr.document.QrType;
import com.libreqr.repository.CampaignQrRepository;
import com.libreqr.repository.CampaignRepository;
import com.libreqr.repository.QrCodeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Humo de repositorios: verifica que las consultas derivadas (paginacion por owner,
 * busqueda por slug, relacion campana-QR) se resuelven y devuelven datos reales.
 */
@SpringBootTest
class RepositorySmokeIT {

    @Autowired
    private QrCodeRepository qrCodes;
    @Autowired
    private CampaignRepository campaigns;
    @Autowired
    private CampaignQrRepository campaignQrs;

    @Test
    void paginacionYRelacionesFuncionan() {
        UUID ownerId = UUID.randomUUID();

        QrCode qr = newQr(ownerId, "slug-" + UUID.randomUUID());
        qrCodes.save(qr);

        Page<QrCode> page = qrCodes.findByOwnerIdAndDeletedAtIsNull(ownerId, PageRequest.of(0, 10));
        assertEquals(1, page.getTotalElements(), "Debe paginar los QRs del owner");
        assertEquals("url", page.getContent().get(0).getType().name());

        QrCode bySlug = qrCodes.findBySlugAndDeletedAtIsNull(qr.getSlug());
        assertTrue(bySlug.getId().equals(qr.getId()), "Busqueda por slug");

        Campaign campaign = new Campaign();
        campaign.setId(UUID.randomUUID());
        campaign.setOwnerId(ownerId);
        campaign.setName("test");
        campaign.setCreatedAt(Instant.now());
        campaign.setUpdatedAt(Instant.now());
        campaigns.save(campaign);

        CampaignQr link = CampaignQr.of(campaign.getId(), qr.getId(), Instant.now());
        campaignQrs.save(link);

        List<CampaignQr> links = campaignQrs.findByCampaignId(campaign.getId());
        assertEquals(1, links.size());
        assertTrue(campaignQrs.existsByCampaignIdAndQrCodeId(campaign.getId(), qr.getId()));

        campaignQrs.deleteByCampaignIdAndQrCodeId(campaign.getId(), qr.getId());
        assertFalse(campaignQrs.existsByCampaignIdAndQrCodeId(campaign.getId(), qr.getId()));

        campaigns.delete(campaign);
        qrCodes.delete(qr);
    }

    private QrCode newQr(UUID ownerId, String slug) {
        QrCode qr = new QrCode();
        qr.setId(UUID.randomUUID());
        qr.setOwnerId(ownerId);
        qr.setType(QrType.url);
        qr.setStatus(QrStatus.active);
        qr.setName("n");
        qr.setSlug(slug);
        qr.setPayload("https://x");
        qr.setCreatedAt(Instant.now());
        qr.setUpdatedAt(Instant.now());
        return qr;
    }
}
