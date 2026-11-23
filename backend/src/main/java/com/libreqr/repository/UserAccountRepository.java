package com.libreqr.repository;

import com.libreqr.document.UserAccount;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

/**
 * Acceso a datos de cuentas de usuario (login y claves).
 */
@Repository
public interface UserAccountRepository extends MongoRepository<UserAccount, UUID> {

    Optional<UserAccount> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);
}
