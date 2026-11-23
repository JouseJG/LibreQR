package com.libreqr.util;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;

/**
 * Clave de visitante (criterio tecnico del documento).
 *
 * <p>Asigna un identificador aleatorio de primera parte y calcula su HMAC-SHA-256 con una
 * clave de servidor: en los eventos se guarda SOLO el hash, nunca el valor original. Si no
 * se configura {@code libreqr.visitor.secret} se deriva una clave aleatoria por arranque
 * (los hashes no seran comparables entre reinicios; en produccion debe fijarse el secreto).</p>
 */
@Component
public class VisitorKeyService {

    private static final SecureRandom RANDOM = new SecureRandom();

    private final SecretKeySpec hmacKey;

    public VisitorKeyService(@Value("${libreqr.visitor.secret:}") String configuredSecret) {
        String secret = configuredSecret;
        if (secret == null || secret.isBlank()) {
            byte[] fallback = new byte[32];
            RANDOM.nextBytes(fallback);
            secret = Base64.getEncoder().encodeToString(fallback);
        }
        this.hmacKey = new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    /** Identificador aleatorio (URL-safe) para la cookie de primera parte. */
    public String newVisitorId() {
        byte[] bytes = new byte[24];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** HMAC-SHA-256 del identificador; eso es lo que se almacena en scan_events. */
    public String hash(String visitorId) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(hmacKey);
            return HexFormat.of().formatHex(mac.doFinal(visitorId.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("No se pudo calcular el hash de visitante", ex);
        }
    }
}
