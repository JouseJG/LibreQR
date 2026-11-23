package com.libreqr.config;

import com.libreqr.security.AuthenticatedUser;
import com.libreqr.security.ApiAuthFilter;
import com.libreqr.security.AuthenticatedUser;
import org.springframework.core.MethodParameter;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;
import org.springframework.web.multipart.MultipartHttpServletRequest;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;
import jakarta.servlet.http.HttpServletRequest;

import java.util.List;

/**
 * Resuelve el argumento {@link AuthenticatedUser} de los controladores desde el atributo que
 * deja el {@link ApiAuthFilter} (identidad verificada en el backend, no un id del cliente).
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    @Override
    public boolean supportsParameter(@NonNull MethodParameter parameter) {
        return parameter.getParameterType().equals(AuthenticatedUser.class);
    }

    @Override
    public Object resolveArgument(@NonNull MethodParameter parameter,
                                  @NonNull ModelAndViewContainer mavContainer,
                                  @NonNull NativeWebRequest webRequest,
                                  WebDataBinderFactory binderFactory) {
        HttpServletRequest request = webRequest.getNativeRequest(HttpServletRequest.class);
        if (request == null) {
            throw new IllegalStateException("Sin request para CurrentUser");
        }
        Object principal = request.getAttribute(ApiAuthFilter.PRINCIPAL_ATTRIBUTE);
        if (!(principal instanceof AuthenticatedUser current)) {
            throw new IllegalStateException("Request sin autenticar");
        }
        return current;
    }
}
