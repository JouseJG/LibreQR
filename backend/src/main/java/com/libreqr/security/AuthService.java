package com.libreqr.security;

import com.libreqr.document.ApiKey;
import com.libreqr.document.Session;
import com.libreqr.document.UserAccount;
import com.libreqr.exception.ConflictException;
import com.libreqr.exception.InvalidContentException;
import com.libreqr.repository.ApiKeyRepository;
import com.libreqr.repository.SessionRepository;
import com.libreqr.repository.UserAccountRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Registro, login y claves de API (Fase 2, decision cookie-servidor + keys).
 *
 * <p>Contrato con el frontend:</p>
 * <ul>
 *   <li>{@code POST /api/auth/register} crea la cuenta (registro abierto; el modo por
 *       invitacion es un decision pendiente).</li>
 *   <li>{@code POST /api/auth/login} emite la cookie {@code libreqr_sid} (solo el id
 *       aleatorio; en BD se guarda su SHA-256).</li>
 *   <li>{@code POST /api/keys} crea una API key (devuelve el valor en claro una sola vez;
 *       en BD solo el hash). Se usa con {@code Authorization: Bearer lqr_...}.</li>
 * </ul>
 *
 * <p>Las contraseñas se guardan con SHA-256 + salt (pendiente migrar a bcrypt/argon2).</p>
 */
@Service
public class AuthService {

    public static final Duration SESSION_TTL = Duration.ofDays(14);

    private final UserAccountRepository users;
    private final SessionRepository sessions;
    private final ApiKeyRepository apiKeys;
    private final CredentialsCrypto crypto;

    @Value("${libreqr.registration.open:true}")
    private boolean openRegistration;

    public AuthService(UserAccountRepository users,
                       SessionRepository sessions,
                       ApiKeyRepository apiKeys,
                       CredentialsCrypto crypto) {
        this.users = users;
        this.sessions = sessions;
        this.apiKeys = apiKeys;
        this.crypto = crypto;
    }

    public UserAccount register(String email, String name, String password) {
        if (!openRegistration) {
            throw new InvalidContentException(
                    "El registro esta cerrado; se necesita invitacion");
        }
        if (users.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("Ya existe una cuenta con ese correo");
        }
        Instant now = Instant.now();
        UserAccount account = new UserAccount();
        account.setId(UUID.randomUUID());
        account.setEmail(email.toLowerCase());
        account.setName(name);
        account.setPasswordSalt(crypto.newSecret(""));
        account.setPasswordHash(crypto.sha256(account.getPasswordSalt() + "|" + password));
        account.setRole(UserAccount.Role.USER);
        account.setCreatedAt(now);
        account.setUpdatedAt(now);
        return users.save(account);
    }

    /** Login: devuelve la cuenta y crea la sesion. El token en claro solo va a la cookie. */
    public LoginResult login(String email, String password) {
        UserAccount account = users.findByEmailIgnoreCase(email)
                .filter(a -> a.getPasswordHash().equals(
                        crypto.sha256(a.getPasswordSalt() + "|" + password)))
                .orElseThrow(() ->
                        new InvalidContentException("Correo o contrasena incorrectos"));

        String token = crypto.newSecret("sid_");
        Session session = new Session();
        session.setId(UUID.randomUUID());
        session.setTokenHash(crypto.sha256(token));
        session.setUserId(account.getId());
        session.setCreatedAt(Instant.now());
        session.setExpiresAt(Instant.now().plus(SESSION_TTL));
        session.setLastSeenAt(Instant.now());
        sessions.save(session);

        return new LoginResult(account, token);
    }

    /** Revoca la sesion actual (logout). */
    public void logout(String sessionToken) {
        if (sessionToken == null) {
            return;
        }
        sessions.findByTokenHash(crypto.sha256(sessionToken)).ifPresent(s -> {
            s.setRevoked(true);
            sessions.save(s);
        });
    }

    /** Crea una API key; el valor en claro se devuelve una unica vez. */
    public CreatedKey createApiKey(UUID userId, String name) {
        String plain = crypto.newSecret(ApiAuthFilter.KEY_PREFIX);
        ApiKey key = new ApiKey();
        key.setId(UUID.randomUUID());
        key.setKeyHash(crypto.sha256(plain));
        key.setDisplay(crypto.displayOf(plain));
        key.setUserId(userId);
        key.setName(name);
        key.setCreatedAt(Instant.now());
        key.setRevoked(false);
        apiKeys.save(key);
        return new CreatedKey(key.getId(), plain, key.getDisplay(), key.getName(),
                key.getCreatedAt());
    }

    public List<ApiKey> listKeys(UUID userId) {
        return apiKeys.findByUserIdAndRevokedFalse(userId);
    }

    /** Revocar clave: solo las del propio usuario (404 si no existe o es ajena). */
    public void revokeKey(UUID userId, UUID keyId) {
        ApiKey key = apiKeys.findById(keyId)
                .filter(k -> k.getUserId().equals(userId))
                .orElseThrow(() -> new com.libreqr.exception.ResourceNotFoundException(
                        "Clave no encontrada: " + keyId));
        key.setRevoked(true);
        apiKeys.save(key);
    }

    /** Resultado del login: cuenta + token de sesion en claro (para la cookie). */
    public record LoginResult(UserAccount account, String sessionToken) {
    }

    /** Clave creada: el frontend debe guardar {@code plain} (no se vuelve a mostrar). */
    public record CreatedKey(UUID id, String plain, String display, String name,
                             Instant createdAt) {
    }
}
