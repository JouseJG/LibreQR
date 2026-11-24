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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
        // Primero creo un QR propio para asociar (la campana base ya es la suya).
        MvcResult qrRes = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"QR campana","payload":"https://c.example",\
                                "campaignId":"%s"}""".formatted(campaignId)))
                .andExpect(status().isCreated())
                .andReturn();
        qrId = JsonPath.read(qrRes.getResponse().getContentAsString(), "$.id");

        // create ya vincula la relacion: anadir de nuevo es 409
        mockMvc.perform(post("/api/campaigns/" + campaignId + "/qrs/" + qrId)
                        .header("X-User-Id", OWNER))
                .andExpect(status().isConflict());

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

    @Test
    @Order(10)
    void buscaPorNombreODescripcion() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Buscable %s","description":"detalles %s"}"""
                                .formatted(tag, tag)))
                .andExpect(status().isCreated());

        mockMvc.perform(get("/api/campaigns").header("X-User-Id", OWNER)
                        .param("search", "buscable " + tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Buscable " + tag));

        mockMvc.perform(get("/api/campaigns").header("X-User-Id", OWNER)
                        .param("search", "DETALLES " + tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/campaigns").header("X-User-Id", OWNER)
                        .param("search", "noexiste-" + tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(11)
    void cambiaYFiltraPorEstado() throws Exception {
        String tag = UUID.randomUUID().toString().substring(0, 8);
        String body = mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Estadillo %s"}""".formatted(tag)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.status").value("active"))
                .andReturn().getResponse().getContentAsString();
        String id = JsonPath.read(body, "$.id");

        mockMvc.perform(patch("/api/campaigns/" + id + "/status")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"paused"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(get("/api/campaigns").header("X-User-Id", OWNER)
                        .param("status", "paused").param("search", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/campaigns").header("X-User-Id", OWNER)
                        .param("status", "active").param("search", tag))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));

        // la edicion (PUT) no toca el estado
        mockMvc.perform(put("/api/campaigns/" + id)
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Estadillo %s v2"}""".formatted(tag)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("paused"));

        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .patch("/api/campaigns/" + id + "/status")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"finished"}"""))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("finished"));
    }

    @Test
    @Order(12)
    void noSePuedenAsignarQrsACampanasNoActivasODuplicarCampana() throws Exception {
        // campana pausada
        String campB = JsonPath.read(mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"name":"Pausada para asignar"}"""))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        mockMvc.perform(patch("/api/campaigns/" + campB + "/status")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"status":"paused"}"""))
                .andExpect(status().isOk());

        // crear un QR directamente en la campana pausada -> 409
        mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"text","name":"sin destino","payload":"hola",\
                                "campaignId":"%s"}""".formatted(campB)))
                .andExpect(status().isConflict());

        // QR de otra campana: vincularlo a la campana pausada -> 409
        mockMvc.perform(post("/api/campaigns/" + campB + "/qrs/" + qrId)
                        .header("X-User-Id", OWNER))
                .andExpect(status().isConflict());
    }

    @Test
    @Order(13)
    void detalleQrsAsociadosConBusquedaYOrden() throws Exception {
        MvcResult campRes = mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Filtro tabla\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String camp = JsonPath.read(campRes.getResponse().getContentAsString(), "$.id");

        for (String name : new String[]{"Alfa tabla", "Beta tabla"}) {
            mockMvc.perform(post("/api/qr")
                            .header("X-User-Id", OWNER)
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(("{\"type\":\"url\",\"name\":\"" + name
                                    + "\",\"payload\":\"https://t.example\",\"campaignId\":\""
                                    + camp + "\"}")))
                    .andExpect(status().isCreated());
        }

        mockMvc.perform(get("/api/campaigns/" + camp + "/qrs/details")
                        .header("X-User-Id", OWNER).param("sort", "name,asc"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(2))
                .andExpect(jsonPath("$.content[0].name").value("Alfa tabla"));

        mockMvc.perform(get("/api/campaigns/" + camp + "/qrs/details")
                        .header("X-User-Id", OWNER).param("search", "Beta"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].name").value("Beta tabla"));

        mockMvc.perform(get("/api/campaigns/" + camp + "/qrs/details")
                        .header("X-User-Id", OWNER).param("page", "1").param("size", "1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    @Order(14)
    void estadisticasDeCampanaYExport() throws Exception {
        MvcResult campRes = mockMvc.perform(post("/api/campaigns")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\":stats\",\"description\":\"d\"}"))
                .andExpect(status().isCreated())
                .andReturn();
        String camp = JsonPath.read(campRes.getResponse().getContentAsString(), "$.id");

        MvcResult qrRes = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(("{\"type\":\"url\",\"name\":\":stats qr\","
                                + "\"payload\":\"https://stats.example\",\"campaignId\":\""
                                + camp + "\"}")))
                .andExpect(status().isCreated())
                .andReturn();
        String slug = JsonPath.read(qrRes.getResponse().getContentAsString(), "$.slug");

        // dos escaneos moviles desde PT y uno de escritorio desde ES
        mockMvc.perform(get("/r/" + slug)
                        .header("User-Agent", "Mozilla/5.0 (iPhone; CPU iPhone OS 17_0 like Mac OS X)"
                                + " AppleWebKit/605.1.15 Mobile/15E148")
                        .header("Accept-Language", "pt-PT,pt;q=0.9"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/r/" + slug)
                        .header("User-Agent", "Mozilla/5.0 (Linux; Android 14; Pixel 8) Mobile")
                        .header("Accept-Language", "pt-PT"))
                .andExpect(status().is3xxRedirection());
        mockMvc.perform(get("/r/" + slug)
                        .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64)")
                        .header("Accept-Language", "es-ES,es;q=0.9"))
                .andExpect(status().is3xxRedirection());

        mockMvc.perform(get("/api/campaigns/" + camp + "/stats").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.scans").value(3))
                .andExpect(jsonPath("$.scansAllTime").value(3))
                .andExpect(jsonPath("$.activeQrs").value(1))
                .andExpect(jsonPath("$.totalQrs").value(1))
                .andExpect(jsonPath("$.deviceTypes[0].key").value("mobile"))
                .andExpect(jsonPath("$.deviceTypes[0].scans").value(2))
                .andExpect(jsonPath("$.countries[0].key").value("PT"))
                .andExpect(jsonPath("$.countries[0].scans").value(2))
                .andExpect(jsonPath("$.dailySeries.length()").value(greaterThanOrEqualTo(1)));

        String csv = mockMvc.perform(get("/api/campaigns/" + camp + "/stats/export")
                        .header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        org.junit.jupiter.api.Assertions.assertTrue(csv.startsWith("fecha,escaneos"));
        org.junit.jupiter.api.Assertions.assertTrue(csv.contains(",3"));
    }
}
