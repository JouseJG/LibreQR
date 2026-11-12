package com.libreqr.repository;

import com.libreqr.document.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

/**
 * Repositorio de usuarios.
 *
 * <p>CRUD básico por ID y búsqueda por email (para autenticación).</p>
 */
@Repository
public interface UserRepository extends MongoRepository<User, UUID> {

    User findByEmail(String email);
}
