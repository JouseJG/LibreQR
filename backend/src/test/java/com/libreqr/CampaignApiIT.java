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
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba end-to-end de la API de campanas contra el Mongo del docker compose:
 * alta, listado paginado, edicion, asociacion de QRs y borrado logico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class CampaignApiIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String OWNER = UUID.randomUUID().toString();
    private static String campaignId;
    private static String qrId;

    @Test
    @Order(1)
    void crearCampana() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Verano 2026","description":"Carteleria de playa"}"""))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Verano 2026"))
                .andReturn();
        campaignId = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    @Test
    @Order(2)
    void rechazaNombreVacio() throws Exception {
        mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    void listaPaginada() throws Exception {
        mockMvc.perform(get("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    @Order(4)
    void editarCampana() throws Exception {
        mockMvc.perform(put("/api/campaigns/" + campaignId)
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Verano 2026 (actualizado)"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Verano 2026 (actualizado)"));
    }

    @Test
    @Order(5)
    void anadirYListarQrs() throws Exception {
        // Primero creo un QR propio para asociar.
        MvcResult qrRes = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"QR campana","payload":"https://c.example"}"""))
                .andExpect(status().isCreated())
                .andReturn();
        qrId = JsonPath.read(qrRes.getResponse().getContentAsString(), "$.id");

        mockMvc.perform(post("/api/campaigns/" + campaignId + "/qrs/" + qrId)
                        .header("X-User-Id", OWNER))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.qrCodeId").value(qrId));

        mockMvc.perform(get("/api/campaigns/" + campaignId + "/qrs")
                        .header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].qrCodeId").value(qrId));
    }

    @Test
    @Order(6)
    void noPermiteDuplicarQr() throws Exception {
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/qrs/" + qrId)
                        .header("X-User-Id", OWNER))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(7)
    void quitarQrNoEliminaElQr() throws Exception {
        mockMvc.perform(delete("/api/campaigns/" + campaignId + "/qrs/" + qrId)
                        .header("X-User-Id", OWNER))
                .andExpect(status().isNoContent());

        // REQ-CAM-05: el QR sigue existiendo tras quitarlo de la campana.
        mockMvc.perform(get("/api/qr/" + qrId).header("X-User-Id", OWNER))
                .andExpect(status().isOk());

        // Y ya no esta en la campana.
        mockMvc.perform(get("/api/campaigns/" + campaignId + "/qrs")
                        .header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    @Order(8)
    void borrarCampanaNosoloAfectaAlQr() throws Exception {
        mockMvc.perform(delete("/api/campaigns/" + campaignId).header("X-User-Id", OWNER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/campaigns/" + campaignId).header("X-User-Id", OWNER))
                .andExpect(status().isNotFound());

        mockMvc.perform(get("/api/qr/" + qrId).header("X-User-Id", OWNER))
                .andExpect(status().isOk()); // el QR sobrevive a la campana
    }

    @Test
    @Order(9)
    void aíslaPorPropietario() throws Exception {
        mockMvc.perform(get("/api/campaigns/" + campaignId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }
}
