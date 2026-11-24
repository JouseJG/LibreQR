package com.libreqr;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.MethodOrderer;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestMethodOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Resumen del dashboard: contadores por estado y escaneos del mes en curso.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class StatsSummaryIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String OWNER = UUID.randomUUID().toString();
    private static final String MOBILE_UA =
            "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X) AppleWebKit/605.1.15 "
                    + "(KHTML, like Gecko) Version/17.0 Mobile/15E148 Safari/604.1";

    private static String campaignId;
    private static String qrActivo;
    private static String qrInactivo;
    private static String slugActivo;

    @Test
    @Order(1)
    void prepararDatos() throws Exception {
        campaignId = JsonPath.read(mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Campana stats"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");

        qrActivo = crearQr("qr-activo-stats");
        qrInactivo = crearQr("qr-inactivo-stats");
        slugActivo = JsonPath.read(mockMvc.perform(get("/api/qr/" + qrActivo)
                        .header("X-User-Id", OWNER))
                .andReturn().getResponse().getContentAsString(), "$.slug");

        mockMvc.perform(patch("/api/qr/" + qrInactivo + "/deactivate").header("X-User-Id", OWNER))
                .andExpect(status().isOk());
    }

    @Test
    @Order(2)
    void summaryReflejaEstadoYEscaneosDelMes() throws Exception {
        // dos escaneos humanos (visitante anonimo + visitante A) y un bot
        mockMvc.perform(get("/r/" + slugActivo).header("User-Agent", MOBILE_UA))
                .andExpect(status().isFound());
        mockMvc.perform(get("/r/" + slugActivo)
                        .cookie(new org.springframework.mock.web.MockCookie(
                                "libreqr_vid", "visitor-A"))
                        .header("User-Agent", MOBILE_UA))
                .andExpect(status().isFound());
        mockMvc.perform(get("/r/" + slugActivo)
                        .header("User-Agent", "qr-scanner-bot/1.0 (compatible; crawler)"))
                .andExpect(status().isFound());

        mockMvc.perform(get("/api/stats/summary").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.activeQrs").value(1))
                .andExpect(jsonPath("$.totalQrs").value(2))
                .andExpect(jsonPath("$.activeCampaigns").value(1))
                .andExpect(jsonPath("$.totalCampaigns").value(1))
                .andExpect(jsonPath("$.scansCurrentMonth").value(2))
                .andExpect(jsonPath("$.uniqueCurrentMonth").value(2))
                .andExpect(jsonPath("$.scansPreviousMonth").value(0));
    }

    @Test
    @Order(3)
    void seriesYComparativaPorCampana() throws Exception {
        mockMvc.perform(get("/api/stats/daily-scans").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].date").exists())
                // el ultimo punto es hoy: los dos escaneos humanos del test
                .andExpect(jsonPath("$[-1].scans").value(2));

        mockMvc.perform(get("/api/stats/campaign-scans").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].name").value("Campana stats"))
                .andExpect(jsonPath("$[0].scans").value(2));
    }

    @Test
    @Order(4)
    void resumenConRangoExplicito() throws Exception {
        java.time.LocalDate today = java.time.LocalDate.now(java.time.ZoneOffset.UTC);
        String from = today.withDayOfMonth(1).toString();
        String to = today.toString();

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/stats/summary")
                        .header("X-User-Id", OWNER)
                        .param("from", from).param("to", to))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scansCurrentMonth").value(2));

        // rango sin eventos (ayer mismo dia si el mes ya tiene >1 dia, o mes anterior)
        String far = today.minusMonths(2).toString();
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .get("/api/stats/summary")
                        .header("X-User-Id", OWNER)
                        .param("from", far).param("to", far))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scansCurrentMonth").value(0));
    }

    private String crearQr(String name) throws Exception {
        return JsonPath.read(mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"%s","payload":"https://stats.example/%s",\
                                "campaignId":"%s"}""".formatted(name, name, campaignId)))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
    }
}
