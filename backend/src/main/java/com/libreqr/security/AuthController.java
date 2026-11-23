package com.libreqr.security;

import com.libreqr.security.AuthService.LoginResult;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Duration;

/**
 * Login/registro/logout publicos (no pasan por ApiAuthFilter: rutas {@code /api/auth/*}).
 *
 * <p>Tras {@code login} el navegador queda con la cookie {@code libreqr_sid} (HttpOnly,
 * SameSite=Lax) y todas las llamadas {@code /api/**} quedan autenticadas sin token visible.
 * Para clientes no-navegador, las claves de API van en {@code Authorization: Bearer}.</p>
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    /** Entrada de login/registro. {@code name} solo se usa en register (opcional). */
    public record Credentials(
            @NotBlank @Email String email,
            @Size(max = 120) String name,
            @NotBlank @Size(min = 8, max = 128) String password
    ) {
    }

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody Credentials credentials) {
        auth.register(credentials.email(), credentials.name(), credentials.password());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/login")
    public ResponseEntity<Void> login(@Valid @RequestBody Credentials credentials,
                                      HttpServletResponse response) {
        LoginResult result = auth.login(credentials.email(), credentials.password());
        ResponseCookie cookie = ResponseCookie.from(ApiAuthFilter.SESSION_COOKIE,
                        result.sessionToken())
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(AuthService.SESSION_TTL)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request,
                                       HttpServletResponse response) {
        String token = readCookie(request, ApiAuthFilter.SESSION_COOKIE);
        auth.logout(token);
        ResponseCookie cleared = ResponseCookie.from(ApiAuthFilter.SESSION_COOKIE, "")
                .httpOnly(true).sameSite("Lax").path("/").maxAge(Duration.ZERO).build();
        response.addHeader(HttpHeaders.SET_COOKIE, cleared.toString());
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    private static String readCookie(HttpServletRequest request, String name) {
        var cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (var c : cookies) {
            if (name.equals(c.getName()) && !c.getValue().isBlank()) {
                return c.getValue();
            }
        }
        return null;
    }
}
