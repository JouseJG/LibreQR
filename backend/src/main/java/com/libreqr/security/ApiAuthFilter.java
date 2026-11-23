package com.libreqr.security;

import com.libreqr.document.ApiKey;
import com.libreqr.document.Session;
import com.libreqr.repository.ApiKeyRepository;
import com.libreqr.repository.SessionRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

/**
 * Autenticacion de la API privada (decisi\u00f3n 2026-11-13: sesion cookie + API keys).
 *
 * <p>Orden de resolucion: cookie de sesion {@code libreqr_sid} y despues header
 * {@code Authorization: Bearer lqr_...}. En ambos casos se compara el <strong>hash</strong>
 * contra la BD, nunca el valor en claro. Pone el {@link AuthenticatedUser} como atributo de la
 * request (que {@code CurrentUserArgumentResolver} convierte en el argumento del controlador)
 * y renueva {@code lastSeenAt} de forma suave (sin bloquear el request).</p>
 *
 * <p>{@code /api/**} sin identidad -&gt; 401. Rutas publicas: {@code /r/**},
 * {@code /actuator/health} y los endpoints {@code /api/auth/*} (login/logout/registro).</p>
 */
@Component
public class ApiAuthFilter extends OncePerRequestFilter {

    public static final String SESSION_COOKIE = "libreqr_sid";
    public static final String PRINCIPAL_ATTRIBUTE = "libreqr.currentUser";
    public static final String KEY_PREFIX = "lqr_";

    private final SessionRepository sessions;
    private final ApiKeyRepository apiKeys;
    private final CredentialsCrypto crypto;

    /** Modo dev: se acepta el header X-User-Id para las pruebas E2E de QR/campa\u00f1as. */
    @Value("${libreqr.dev.allow-header-owner:false}")
    private boolean devHeaderOwnerAllowed;

    public ApiAuthFilter(SessionRepository sessions,
                         ApiKeyRepository apiKeys,
                         CredentialsCrypto crypto) {
        this.sessions = sessions;
        this.apiKeys = apiKeys;
        this.crypto = crypto;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        if (path == null || path.isBlank()) {
            return true;
        }
        return !path.startsWith("/api/")
                || path.startsWith("/api/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        UUID userId = resolveUser(request);
        if (userId == null) {
            if (devHeaderOwnerAllowed) {
                String headerOwner = request.getHeader("X-User-Id");
                if (headerOwner != null && !headerOwner.isBlank()) {
                    userId = UUID.fromString(headerOwner);
                }
            }
        }
        if (userId == null) {
            writeUnauthorized(response);
            return;
        }
        request.setAttribute(PRINCIPAL_ATTRIBUTE, new AuthenticatedUser(userId));
        filterChain.doFilter(request, response);
    }

    private UUID resolveUser(HttpServletRequest request) {
        UUID fromSession = sessionUser(request);
        if (fromSession != null) {
            return fromSession;
        }
        return bearerKeyUser(request);
    }

    private UUID sessionUser(HttpServletRequest request) {
        String token = readCookie(request, SESSION_COOKIE);
        if (token == null) {
            return null;
        }
        Optional<Session> found = sessions.findByTokenHash(crypto.sha256(token));
        if (found.isEmpty()) {
            return null;
        }
        Session s = found.get();
        if (s.isRevoked()) {
            return null;
        }
        if (s.getExpiresAt() != null && s.getExpiresAt().isBefore(Instant.now())) {
            return null;
        }
        s.setLastSeenAt(Instant.now());
        sessions.save(s);
        return s.getUserId();
    }

    private UUID bearerKeyUser(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            return null;
        }
        String key = header.substring(7).trim();
        if (!key.startsWith(KEY_PREFIX)) {
            return null;
        }
        Optional<ApiKey> found = apiKeys.findByKeyHash(crypto.sha256(key));
        if (found.isEmpty()) {
            return null;
        }
        ApiKey k = found.get();
        if (k.isRevoked() || (k.getExpiresAt() != null && k.getExpiresAt().isBefore(Instant.now()))) {
            return null;
        }
        k.setLastUsedAt(Instant.now());
        apiKeys.save(k);
        return k.getUserId();
    }

    private static String readCookie(HttpServletRequest request, String name) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie c : cookies) {
            if (name.equals(c.getName()) && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }

    private void writeUnauthorized(HttpServletResponse response) throws IOException {
        response.setStatus(HttpStatus.UNAUTHORIZED.value());
        response.setContentType("application/json");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(
                HttpStatus.UNAUTHORIZED, "Autenticación requerida");
        pd.setType(java.net.URI.create("urn:libreqr:error:unauthenticated"));
        pd.setTitle("Unauthorized");
        response.getWriter().write(pd.toString());
    }
}
