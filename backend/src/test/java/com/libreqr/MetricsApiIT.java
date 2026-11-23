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
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba E2E de redirección y métricas. Cada test limpia los eventos de su QR antes de
 * escanear, de modo que las cuentas no dependen del estado acumulado en el Mongo de
 * desarrollo.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class MetricsApiIT {

    private static final String OWNER = UUID.randomUUID().toString();
    private static final String MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";
    private static final String BOT_UA = "qr-scanner-bot/1.0 (compatible; crawler)";

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private MongoTemplate mongo;

    private static String qrId;
    private static String slug;
    private static String campaignId;

    private String createQr(String name) throws Exception {
        MvcResult res = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                               {"type":"url","name":"%s","payload":"https://destino.example/%s",\
                                "campaignId":"%s"}
                               """.formatted(name, name, campaignId)))
                .andExpect(status().isCreated())
                .andReturn();
        return JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    private String slugOf(String id) throws Exception {
        return JsonPath.read(
                mockMvc.perform(get("/api/qr/" + id).header("X-User-Id", OWNER))
                        .andReturn().getResponse().getContentAsString(),
                "$.slug");
    }

    private void purge(UUID qrId) {
        mongo.remove(Query.query(Criteria.where("qrCodeId").is(qrId)), "scan_events");
    }

    private void scan(String slug, String visitorCookie, String userAgent) throws Exception {
        var req = get("/r/" + slug);
        if (visitorCookie != null) {
            req.cookie(new Cookie("libreqr_vid", visitorCookie));
        }
        mockMvc.perform(req.header("User-Agent", userAgent))
                .andExpect(status().isFound());
    }

    @Test
    @Order(0)
    void prepararCampanaBase() throws Exception {
        MvcResult camp = mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Campana metricas"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        campaignId = JsonPath.read(camp.getResponse().getContentAsString(), "$.id");
    }

    @Test
    @Order(1)
    void escaneoRedirigeYAsignaCookie() throws Exception {
        qrId = createQr("qr-metricas");
        slug = slugOf(qrId);
        purge(UUID.fromString(qrId));

        var res = mockMvc.perform(get("/r/" + slug).header("User-Agent", MOBILE_UA))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", "https://destino.example/qr-metricas"))
                .andReturn();

        String setCookie = res.getResponse().getHeader("Set-Cookie");
        org.junit.jupiter.api.Assertions.assertNotNull(setCookie, "debe asignar cookie de visitante");
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("HttpOnly"));
        org.junit.jupiter.api.Assertions.assertTrue(setCookie.contains("SameSite=Lax"));
    }

    @Test
    @Order(2)
    void metricasDistinguenBotsYUnicos() throws Exception {
        purge(UUID.fromString(qrId)); // partimos de cero (el scan de order-1 se descarta)

        scan(slug, null, MOBILE_UA);        // humano anonimo (cookie nueva)
        scan(slug, "visitor-A", MOBILE_UA); // humano A
        scan(slug, "visitor-A", MOBILE_UA); // humano A repetido -> mismo hash
        scan(slug, null, BOT_UA);           // bot

        mockMvc.perform(get("/api/qr/" + qrId + "/metrics").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(4))
                .andExpect(jsonPath("$.humanScans").value(3))
                .andExpect(jsonPath("$.botScans").value(1))
                .andExpect(jsonPath("$.uniqueVisitors").value(2)) // anonima + A
                .andExpect(jsonPath("$.busiestDay").exists())
                .andExpect(jsonPath("$.busiestHour").exists())
                .andExpect(jsonPath("$.firstScanAt").exists())
                .andExpect(jsonPath("$.lastScanAt").exists())
                .andExpect(jsonPath("$.previousPeriodScans").value(0))
                .andExpect(jsonPath("$.byDevice.mobile").value(3))
                .andExpect(jsonPath("$.byCountry").isEmpty()); // sin geoip: no inventar
    }

    @Test
    @Order(3)
    void qrDesactivadoDevuelve410YNoRedirige() throws Exception {
        purge(UUID.fromString(qrId));

        mockMvc.perform(patch("/api/qr/" + qrId + "/deactivate").header("X-User-Id", OWNER))
                .andExpect(status().isOk());

        // El escaneo de un QR desactivado se registra y responde 410 con aviso (REQ-QR-09).
        mockMvc.perform(get("/r/" + slug).header("User-Agent", MOBILE_UA))
                .andExpect(status().isGone())
                .andExpect(jsonPath("$", org.hamcrest.Matchers.not(org.hamcrest.Matchers.empty())))
                .andReturn();

        String body = mockMvc.perform(get("/r/" + slug)
                        .cookie(new Cookie("libreqr_vid", "visitor-A"))
                        .header("User-Agent", MOBILE_UA))
                .andExpect(status().isGone())
                .andExpect(content().string(org.hamcrest.Matchers.containsStringIgnoringCase("operativo")))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    @Order(4)
    void slugInexistenteDevuelve404() throws Exception {
        mockMvc.perform(get("/r/noexiste1").header("User-Agent", MOBILE_UA))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(5)
    void metricasDeCampanaAgreganQrs() throws Exception {
        purge(UUID.fromString(qrId));

        // qrId y qr2 pertenecen a la campana base (asignacion obligatoria en la creacion)
        String qr2 = createQr("qr-campana");
        purge(UUID.fromString(qr2));

        // qr1 (desactivado): 1 escaneo -> 410. qr2: 1 escaneo humano.
        mockMvc.perform(get("/r/" + slug).header("User-Agent", MOBILE_UA))
                .andExpect(status().isGone());
        scan(slugOf(qr2), "visitor-B", MOBILE_UA);

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/metrics").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(2))
                .andExpect(jsonPath("$.humanScans").value(2))
                .andExpect(jsonPath("$.botScans").value(0))
                .andExpect(jsonPath("$.uniqueVisitors").value(2)) // anonima + B
                .andExpect(jsonPath("$.mostScanned.mostName").exists())
                .andExpect(jsonPath("$.byDevice").isMap())
                .andExpect(jsonPath("$.activeWithScansPercent").value(org.hamcrest.Matchers.greaterThan(0.0)));
    }

    @Test
    @Order(6)
    void sinVisitasEstadoVacio() throws Exception {
        String qrNuevo = createQr("qr-sin-visitas");
        purge(UUID.fromString(qrNuevo));

        mockMvc.perform(get("/api/qr/" + qrNuevo + "/metrics").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalScans").value(0))
                .andExpect(jsonPath("$.uniqueVisitors").value(0))
                .andExpect(jsonPath("$.busiestDay").doesNotExist())
                .andExpect(jsonPath("$.firstScanAt").doesNotExist());
    }
}
