package com.libreqr.security;

import com.libreqr.document.ApiKey;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Claves de API del usuario autenticado (protegidas por sesion, no por la propia clave).
 *
 * <p>El frontend usa estas claves luego en {@code Authorization: Bearer lqr_...} para
 * llamadas desde contextos sin cookie (integraciones, scripts). El valor en claro se
 * devuelve SOLO en la respuesta de creacion.</p>
 */
@RestController
@RequestMapping("/api/keys")
public class KeyController {

    /** Entrada de creacion de clave. */
    public record NewKeyRequest(
            @NotBlank @Size(min = 1, max = 120) String name
    ) {
    }

    /** Clave visible una sola vez (no se puede volver a leer). */
    public record CreatedKeyResponse(UUID id, String key, String display, String name,
                                     Instant createdAt) {
    }

    /** Resumen gestionable (nunca el valor en claro ni el hash). */
    public record KeySummary(UUID id, String display, String name, Instant createdAt,
                             Instant lastUsedAt) {
    }

    private final AuthService auth;

    public KeyController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping
    public ResponseEntity<CreatedKeyResponse> create(@CurrentUser AuthenticatedUser current,
                                                     @RequestBody NewKeyRequest request) {
        AuthService.CreatedKey created = auth.createApiKey(current.userId(), request.name());
        return ResponseEntity.status(HttpStatus.CREATED).body(new CreatedKeyResponse(
                created.id(), created.plain(), created.display(), created.name(),
                created.createdAt()));
    }

    @GetMapping
    public List<KeySummary> list(@CurrentUser AuthenticatedUser current) {
        return auth.listKeys(current.userId()).stream().map(this::toSummary).toList();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> revoke(@CurrentUser AuthenticatedUser current, @PathVariable UUID id) {
        auth.revokeKey(current.userId(), id);
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private KeySummary toSummary(ApiKey key) {
        return new KeySummary(key.getId(), key.getDisplay(), key.getName(),
                key.getCreatedAt(), key.getLastUsedAt());
    }
}
