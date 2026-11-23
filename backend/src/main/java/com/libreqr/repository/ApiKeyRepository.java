package com.libreqr.repository;

import com.libreqr.document.ApiKey;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repositorio de claves de API. Busqueda por hash (la API nunca envia claves en claro).
 */
@Repository
public interface ApiKeyRepository extends MongoRepository<ApiKey, UUID> {

    Optional<ApiKey> findByKeyHash(String keyHash);

    List<ApiKey> findByUserIdAndRevokedFalse(UUID userId);
}
