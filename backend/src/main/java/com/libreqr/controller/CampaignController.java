package com.libreqr.controller;

import com.libreqr.document.CampaignStatus;
import com.libreqr.dto.CampaignMetricsResponse;
import com.libreqr.dto.CampaignQrResponse;
import com.libreqr.dto.CampaignResponse;
import com.libreqr.dto.CampaignStatusRequest;
import com.libreqr.dto.CampaignUpsertRequest;
import com.libreqr.service.CampaignQrService;
import com.libreqr.service.CampaignService;
import com.libreqr.service.MetricsService;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * API REST de campanas y de su asociacion con QRs.
 *
 * <p>La identidad la fija {@link CurrentUser} (cookie de sesión o Bearer key); el
 * cliente ya no manda el ownerId.</p>
 */
@RestController
@RequestMapping("/api/campaigns")
public class CampaignController {

    private final CampaignService service;
    private final CampaignQrService qrLinks;
    private final MetricsService metrics;

    public CampaignController(CampaignService service, CampaignQrService qrLinks,
                              MetricsService metrics) {
        this.service = service;
        this.qrLinks = qrLinks;
        this.metrics = metrics;
    }

    @PostMapping
    public ResponseEntity<CampaignResponse> create(
            @CurrentUser AuthenticatedUser current,
            @Valid @RequestBody CampaignUpsertRequest request) {
        CampaignResponse created = service.create(current.userId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<CampaignResponse> list(
            @CurrentUser AuthenticatedUser current,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) CampaignStatus status) {
        return service.list(current.userId(), pageable, search, status);
    }

    @PatchMapping("/{id}/status")
    public CampaignResponse changeStatus(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @Valid @RequestBody CampaignStatusRequest request) {
        return service.changeStatus(current.userId(), id, request.status());
    }

    @GetMapping("/{id}")
    public CampaignResponse get(@CurrentUser AuthenticatedUser current,
                                @PathVariable UUID id) {
        return service.get(current.userId(), id);
    }

    @PutMapping("/{id}")
    public CampaignResponse update(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @Valid @RequestBody CampaignUpsertRequest request) {
        return service.update(current.userId(), id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthenticatedUser current,
                                       @PathVariable UUID id) {
        service.delete(current.userId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // --- Asociacion campana-QR (REQ-CAM-02) ---

    @PostMapping("/{id}/qrs/{qrId}")
    public ResponseEntity<CampaignQrResponse> addQr(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @PathVariable UUID qrId) {
        service.get(current.userId(), id); // 404 si la campana no existe o no es propia
        CampaignQrResponse link = qrLinks.add(current.userId(), id, qrId);
        return ResponseEntity.status(HttpStatus.CREATED).body(link);
    }

    @GetMapping("/{id}/qrs")
    public List<CampaignQrResponse> listQrs(@CurrentUser AuthenticatedUser current,
                                            @PathVariable UUID id) {
        service.get(current.userId(), id); // valida propiedad
        return qrLinks.list(id);
    }

    @DeleteMapping("/{id}/qrs/{qrId}")
    public ResponseEntity<Void> removeQr(@CurrentUser AuthenticatedUser current,
                                         @PathVariable UUID id,
                                         @PathVariable UUID qrId) {
        service.get(current.userId(), id); // valida propiedad (quitar no elimina el QR: REQ-CAM-05)
        qrLinks.remove(id, qrId);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    // --- Metricas agregadas (REQ-CAM-04, REQ-MET-04) ---

    @GetMapping("/{id}/metrics")
    public CampaignMetricsResponse metrics(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam(required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        service.get(current.userId(), id); // valida propiedad
        return metrics.campaignMetrics(current.userId(), id, from, to);
    }
}
