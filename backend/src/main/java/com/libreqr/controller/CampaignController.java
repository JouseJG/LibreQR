package com.libreqr.controller;

import com.libreqr.dto.CampaignQrResponse;
import com.libreqr.dto.CampaignResponse;
import com.libreqr.dto.CampaignUpsertRequest;
import com.libreqr.service.CampaignQrService;
import com.libreqr.service.CampaignService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;
import java.util.UUID;

/**
 * API REST de campanas y de su asociacion con QRs.
 *
 * <p>El {@code ownerId} se pasa por header {@code X-User-Id} de forma provisional hasta la
 * fase de autenticacion (Fase 2).</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService service;
    private final CampaignQrService qrLinks;

    public CampaignController(CampaignService service, CampaignQrService qrLinks) {
        this.service = service;
        this.qrLinks = qrLinks;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @RequestHeader("X-User-Id") UUID ownerId,
            @Valid @RequestBody CampaignUpsertRequest request) {
        CampaignResponse created = service.create(ownerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<CampaignResponse> list(
            @RequestHeader("X-User-Id") UUID ownerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.list(ownerId, pageable);
    }

    @GetMapping("/{id}")
    public CampaignResponse get(@RequestHeader("X-User-Id") UUID ownerId,
                                @PathVariable UUID id) {
        return service.get(ownerId, id);
    }

    @PutMapping("/{id}")
    public CampaignResponse update(
            @RequestHeader("X-User-Id") UUID ownerId,
            @PathVariable UUID id,
            @Valid @RequestBody CampaignUpsertRequest request) {
        return service.update(ownerId, id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") UUID ownerId,
                                       @PathVariable UUID id) {
        service.delete(ownerId, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // --- Asociacion campana-QR (REQ-CAM-02) ---

    @PostMapping("/{id}/qrs/{qrId}")
    public ResponseEntity<CampaignQrResponse> addQr(
            @RequestHeader("X-User-Id") UUID ownerId,
            @PathVariable UUID id,
            @PathVariable UUID qrId) {
        service.get(ownerId, id); // 404 si la campana no existe o no es propia
        CampaignQrResponse link = qrLinks.add(ownerId, id, qrId);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping("/{id}/qrs")
    public List<CampaignQrResponse> listQrs(@RequestHeader("X-User-Id") UUID ownerId,
                                            @PathVariable UUID id) {
        service.get(ownerId, id); // valida propiedad
        return qrLinks.list(id);
    }

    @DeleteMapping("/{id}/qrs/{qrId}")
    public ResponseEntity<Void> removeQr(@RequestHeader("X-User-Id") UUID ownerId,
                                         @PathVariable UUID id,
                                         @PathVariable UUID qrId) {
        service.get(ownerId, id); // valida propiedad (quitar no elimina el QR: REQ-CAM-05)
        qrLinks.remove(id, qrId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
