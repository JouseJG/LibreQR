package com.libreqr.util;

import org.springframework.stereotype.Component;

import java.security.SecureRandom;

/**
 * Generador de slugs de redireccion para QRs dinamicos.
 */
@Component
public class SlugGenerator {

    private static final char[] ALPHABET =
            "abcdefghijkmnpqrstuvwxyz23456789".toCharArray(); // sin l,o,0,1 (ambiguos)
    private static final int LENGTH = 10;
    private final SecureRandom random = new SecureRandom();

    public String generate() {
        StringBuilder sb = new StringBuilder(LENGTH);
        for (int i = 0; i < LENGTH; i++) {
            sb.append(ALPHABET[random.nextInt(ALPHABET.length)]);
        }
        return sb.toString();
    }
}
