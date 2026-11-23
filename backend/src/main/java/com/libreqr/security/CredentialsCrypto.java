package com.libreqr.security;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * Utilidades criptograficas de autenticacion.
 *
 * <p>Los secretos (tokens de sesion, claves de API) se guardan SIEMPRE como digest
 * SHA-256; el valor en claro solo existe en la respuesta de creacion de la clave o en la
 * cookie enviada al navegador.</p>
 */
@Component
public class CredentialsCrypto {

    private static final SecureRandom RANDOM = new SecureRandom();

    /** Token opaco de 32 bytes hex, pre-fijable (p. ej. {@code lqr_} para keys). */
    public String newSecret(String prefix) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        return prefix + HexFormat.of().formatHex(bytes);
    }

    public String sha256(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 no disponible", ex);
        }
    }

    /** Prefijo mostrable de una clave (primeros 12 chars) para distinguirla en la UI. */
    public String displayOf(String plainKey) {
        return plainKey.length() > 16 ? plainKey.substring(0, 16) + "..." : plainKey;
    }
}
