package com.libreqr;

import org.bson.Document;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.mongodb.core.MongoTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 * Prueba de humo de la conexión a MongoDB.
 *
 * <p>Verifica que el contexto arranca y que el {@link MongoTemplate} queda cableado y
 * puede operar (ping). No prueba consultas de negocio: solo que el "mongo connect"
 * está disponible para los futuros repositorios.</p>
 *
 * <p>Requiere MongoDB accesible (usa la configuración de {@code application.yml},
 * p. ej. {@code MONGO_URI} o el Mongo local de {@code docker compose up}).</p>
 */
@SpringBootTest
class MongoConnectionIT {

    @Autowired
    private MongoTemplate mongoTemplate;

    @Test
    void mongoTemplateIsWiredAndConnects() {
        assertNotNull(mongoTemplate, "El bean MongoTemplate debe estar disponible");

        Document ping = mongoTemplate.getDb().runCommand(new Document("ping", 1));

        assertEquals(1.0, ping.get("ok"),
                "El ping a MongoDB debe responder ok=1");
    }
}
