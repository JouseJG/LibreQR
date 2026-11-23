package com.libreqr;

import com.jayway.jsonpath.JsonPath;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E de autenticacion: registro, login con cookie de sesion, creacion de API keys y
 * acceso con Bearer. Prueba ademas que una clave revocada deja de funcionar.
 *
 * <p>Usa credenciales unicas por ejecucion (UUID en el correo) para no chocar con los
 * datos del Mongo de desarrollo.</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class AuthApiIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String EMAIL = "user-" + UUID.randomUUID() + "@libreqr.test";
    private static final String PASSWORD = "secrets-de-prueba";

    private static String sessionCookie;
    private static String plainKey;
    private static String keyId;

    @Test
    @Order(1)
    void registerRechazaCorreoRepetido() throws Exception {
        String body = """
               {"email":"%s","name":"Probe","password":"%s"}
               """.formatted(EMAIL, PASSWORD);
        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNoContent());

        mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(2)
    void loginDevuelveCookieDeSesion() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                               {"email":"%s","name":"","password":"%s"}
                               """.formatted(EMAIL, PASSWORD)))
                .andExpect(status().isNoContent())
                .andReturn();

        String setCookie = res.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie);
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        sessionCookie = setCookie.split(";", 2)[0]; // libreqr_sid=sid_...
    }

    @Test
    @Order(2)
    void sinCredencialesApiDevuelve401() throws Exception {
        // ojo: el filtro solo acepta X-User-Id si la propiedad dev esta activada en test;
        // aqui probamos lo contrario: sin cookie y sin bearer debe ser 401.
        mockMvc.perform(get("/api/qr"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(3)
    void loginIncorrectoRechaza() throws Exception {
        mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"%s","name":"","password":"malisimo"}"""
                                .formatted(EMAIL)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void cookieDeSesionAutentica() throws Exception {
        String name = "ses-" + sessionCookie.substring(sessionCookie.length() - 6);

        // todo QR nace en una campana: creamos una con la misma sesion
        String campaignId = JsonPath.read(mockMvc.perform(post("/api/campaigns")
                        .cookie(parseCookie(sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Campana sesion"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/qr")
                        .cookie(parseCookie(sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"%s","payload":"https://sesion.example",\
                                "campaignId":"%s"}""".formatted(name, campaignId)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.slug").isNotEmpty());
    }

    @Test
    @Order(5)
    void crearYUsarApiKey() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/keys")
                        .cookie(parseCookie(sessionCookie))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Frontend E2E"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.key").exists())
                .andReturn();

        plainKey = JsonPath.read(res.getResponse().getContentAsString(), "$.key");
        keyId = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
        org.junit.jupiter.api.Assertions.assertTrue(plainKey.startsWith("lqr_"));

        // La key en claro ya no se puede volver a leer (list -> solo display).
        mockMvc.perform(get("/api/keys").cookie(parseCookie(sessionCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].display").exists())
                .andExpect(jsonPath("$[0].key").doesNotExist());

        // Uso de la API key via Bearer.
        mockMvc.perform(get("/api/qr")
                        .header("Authorization", "Bearer " + plainKey))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @Order(6)
    void revocarKeyDejaDeFuncionar() throws Exception {
        mockMvc.perform(delete("/api/keys/" + keyId).cookie(parseCookie(sessionCookie)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/qr")
                        .header("Authorization", "Bearer " + plainKey))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @Order(7)
    void logoutInvalidaSesion() throws Exception {
        mockMvc.perform(post("/api/auth/logout").cookie(parseCookie(sessionCookie)))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/qr").cookie(parseCookie(sessionCookie)))
                .andExpect(status().isUnauthorized());
    }

    private static Cookie parseCookie(String setCookieHeader) {
        String pair = setCookieHeader.split(";", 2)[0];
        int eq = pair.indexOf('=');
        return new Cookie(pair.substring(0, eq).trim(), pair.substring(eq + 1).trim());
    }
}
