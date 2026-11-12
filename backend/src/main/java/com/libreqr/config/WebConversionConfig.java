package com.libreqr.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.format.FormatterRegistry;
import org.springframework.core.convert.converter.Converter;
import org.springframework.lang.NonNull;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.UUID;

/**
 * Conversion de tipos de dominio (UUID e Instant) en los argumentos de los endpoints.
 *
 * <p>Ambos tipos son comunes en las entidades y se reciben como strings desde la API.</p>
 */
@Configuration
public class WebConversionConfig implements WebMvcConfigurer {

    @Override
    public void addFormatters(@NonNull FormatterRegistry registry) {
        registry.addConverter(new UuidStringConverter());
        registry.addConverter(new InstantStringConverter());
    }

    private static final class UuidStringConverter implements Converter<String, UUID> {
        @Override
        public UUID convert(@NonNull String source) {
            return UUID.fromString(source);
        }
    }

    private static final class InstantStringConverter implements Converter<String, Instant> {
        @Override
        public Instant convert(@NonNull String source) {
            try {
                return Instant.parse(source);
            } catch (DateTimeParseException ex) {
                throw new IllegalArgumentException("Formato de fecha inválido: " + source, ex);
            }
        }
    }
}
