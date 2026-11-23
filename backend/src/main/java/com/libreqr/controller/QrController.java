package com.libreqr.controller;

import com.libreqr.document.QrStatus;
import com.libreqr.dto.QrResponse;
import com.libreqr.dto.QrUpsertRequest;
import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.CurrentUser;
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
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.UUID;

/**
 * API REST de QRs. La identidad la fija {@link CurrentUser} (cookie de sesion o Bearer
 * key); el cliente ya no manda el ownerId.
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
            @CurrentUser AuthenticatedUser current,
            @Valid @RequestBody QrUpsertRequest request) {
        QrResponse created = service.create(current.userId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}").buildAndExpand(created.id()).toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping
    public Page<QrResponse> list(
            @CurrentUser AuthenticatedUser current,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) QrStatus status) {
        return service.list(current.userId(), pageable, search, status);
    }

    @GetMapping("/{id}")
    public QrResponse get(@CurrentUser AuthenticatedUser current, @PathVariable UUID id) {
        return service.get(current.userId(), id);
    }

    @PutMapping("/{id}")
    public QrResponse update(
            @CurrentUser AuthenticatedUser current,
            @PathVariable UUID id,
            @Valid @RequestBody QrUpsertRequest request) {
        return service.update(current.userId(), id, request);
    }

    @PatchMapping("/{id}/deactivate")
    public QrResponse deactivate(@CurrentUser AuthenticatedUser current, @PathVariable UUID id) {
        return service.deactivate(current.userId(), id);
    }

    @PatchMapping("/{id}/reactivate")
    public QrResponse reactivate(@CurrentUser AuthenticatedUser current, @PathVariable UUID id) {
        return service.reactivate(current.userId(), id);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@CurrentUser AuthenticatedUser current, @PathVariable UUID id) {
        service.delete(current.userId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }
}
