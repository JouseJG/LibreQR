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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba end-to-end de la API de QRs contra el Mongo del docker compose.
 * Cubre alta, listado paginado, consulta, edicion, desactivacion y borrado logico.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class QrApiIT {

    @Autowired
    private MockMvc mockMvc;

    private static final String OWNER = UUID.randomUUID().toString();
    private static String qrId;

    private static String body(String name, String payload) {
        return """
               {"type":"url","name":"%s","payload":"%s","designConfig":{"frame":"rounded"}}
               """.formatted(name, payload);
    }

    @Test
    @Order(1)
    void crearQrValido() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mi QR", "https://libreqr.example")))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").exists())
                .andExpect(jsonPath("$.slug").isNotEmpty())
                .andExpect(jsonPath("$.editable").value(true))
                .andReturn();
        qrId = com.jayway.jsonpath.JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    @Test
    @Order(2)
    void rechazaEnlaceInvalido() throws Exception {
        mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("malo", "ftp://no-http")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail", containsString("http")));
    }

    @Test
    @Order(3)
    void rechazaPayloadVacio() throws Exception {
        mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"x","payload":""}"""))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    void listaPaginada() throws Exception {
        mockMvc.perform(get("/api/qr")
                        .header("X-User-Id", OWNER)
                        .param("page", "0").param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements", greaterThanOrEqualTo(1)))
                .andExpect(jsonPath("$.content[0].name").exists());
    }

    @Test
    @Order(5)
    void editarDestinoSinCambiarSlug() throws Exception {
        String before = mockMvc.perform(get("/api/qr/" + qrId).header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String slugBefore = com.jayway.jsonpath.JsonPath.read(before, "$.slug");

        mockMvc.perform(put("/api/qr/" + qrId)
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body("Mi QR", "https://nuevo-destino.example")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.payload").value("https://nuevo-destino.example"))
                .andExpect(jsonPath("$.slug").value(slugBefore));
    }

    @Test
    @Order(6)
    void desactivarYEliminar() throws Exception {
        mockMvc.perform(patch("/api/qr/" + qrId + "/deactivate").header("X-User-Id", OWNER))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("inactive"));

        mockMvc.perform(delete("/api/qr/" + qrId).header("X-User-Id", OWNER))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/qr/" + qrId).header("X-User-Id", OWNER))
                .andExpect(status().isNotFound());
    }

    @Test
    @Order(7)
    void aíslaPorPropietario() throws Exception {
        mockMvc.perform(get("/api/qr/" + qrId)
                        .header("X-User-Id", UUID.randomUUID()))
                .andExpect(status().isNotFound());
    }

    // --- Filtros de busqueda/estado y conteo por lotes ---

    private static final String TAG = UUID.randomUUID().toString().substring(0, 8);
    private static String filtrableId;

    @Test
    @Order(8)
    void crearQrFiltrable() throws Exception {
        MvcResult res = mockMvc.perform(post("/api/qr")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"type":"url","name":"Buscable %s",\
                                "description":"unicorno-%s","payload":"https://libreqr.example"}"""
                                .formatted(TAG, TAG)))
                .andExpect(status().isCreated())
                .andReturn();
        filtrableId = JsonPath.read(res.getResponse().getContentAsString(), "$.id");
    }

    @Test
    @Order(9)
    void buscaPorNombreYDescripcion() throws Exception {
        mockMvc.perform(get("/api/qr").header("X-User-Id", OWNER)
                        .param("search", "buscable " + TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1))
                .andExpect(jsonPath("$.content[0].id").value(filtrableId));

        mockMvc.perform(get("/api/qr").header("X-User-Id", OWNER)
                        .param("search", "UNICORNO-" + TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/qr").header("X-User-Id", OWNER)
                        .param("search", "noexiste-" + TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(10)
    void filtraPorEstado() throws Exception {
        mockMvc.perform(get("/api/qr").header("X-User-Id", OWNER)
                        .param("status", "active").param("search", TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(1));

        mockMvc.perform(get("/api/qr").header("X-User-Id", OWNER)
                        .param("status", "inactive").param("search", TAG))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    @Order(11)
    void cuentaEscaneosPorLotes() throws Exception {
        mockMvc.perform(post("/api/qr/scan-counts")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":["%s","%s"]}""".formatted(filtrableId, UUID.randomUUID())))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.['" + filtrableId + "']").value(0));

        // Ids ajenos no se devuelven (el frontend mostrara 0 igualmente).
        mockMvc.perform(post("/api/qr/scan-counts")
                        .header("X-User-Id", UUID.randomUUID().toString())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":["%s"]}""".formatted(filtrableId)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());

        mockMvc.perform(post("/api/qr/scan-counts")
                        .header("X-User-Id", OWNER)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"ids":[]}"""))
                .andExpect(status().isBadRequest());
    }
}
