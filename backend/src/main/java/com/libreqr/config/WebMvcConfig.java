package com.libreqr.config;

import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

/**
 * Registra los resolvedores propios en WebMVC (CurrentUser).
 */
@org.springframework.stereotype.Component
public class WebMvcConfig implements WebMvcConfigurer {

    private final List<HandlerMethodArgumentResolver> customResolvers;

    public WebMvcConfig(List<HandlerMethodArgumentResolver> customResolvers) {
        this.customResolvers = customResolvers;
    }

    @Override
    public void addArgumentResolvers(@org.springframework.lang.NonNull
                                     List<HandlerMethodArgumentResolver> resolvers) {
        customResolvers.stream()
                .filter(r -> r instanceof CurrentUserArgumentResolver)
                .findFirst()
                .ifPresent(resolvers::add);
    }
}
