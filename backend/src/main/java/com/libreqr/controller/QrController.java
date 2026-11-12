package com.libreqr.controller;

import com.libreqr.dto.QrResponse;
import com.libreqr.dto.QrUpsertRequest;
import com.libreqr.service.QrService;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * API REST de QRs.
 *
 * <p>El {@code ownerId} se pasa por header {@code X-User-Id} de forma provisional: es un
 * sustituto de la autenticacion real (Fase 2), donde se obtendra del token y se verificara
 * igualmente en el backend.</p>
 */
@RestController
@RequestMapping("/api/qr")
public class QrController {

    private final QrService service;

    public QrController(QrService service) {
        this.service = service;
    }

    @PostMapping
    public ResponseEntity<QrResponse> create(
            @RequestHeader("X-User-Id") UUID ownerId,
            @Valid @RequestBody QrUpsertRequest request) {
        QrResponse created = service.create(ownerId, request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<QrResponse> list(
            @RequestHeader("X-User-Id") UUID ownerId,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {
        return service.list(ownerId, pageable);
    }

    @GetMapping("/{id}")
    public QrResponse get(@RequestHeader("X-User-Id") UUID ownerId, @PathVariable UUID id) {
        return service.get(ownerId, id);
    }

    @PutMapping("/{id}")
    public QrResponse update(
            @RequestHeader("X-User-Id") UUID ownerId,
            @PathVariable UUID id,
            @Valid @RequestBody QrUpsertRequest request) {
        return service.update(ownerId, id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public QrResponse deactivate(@RequestHeader("X-User-Id") UUID ownerId, @PathVariable UUID id) {
        return service.deactivate(ownerId, id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@RequestHeader("X-User-Id") UUID ownerId, @PathVariable UUID id) {
        service.delete(ownerId, id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
