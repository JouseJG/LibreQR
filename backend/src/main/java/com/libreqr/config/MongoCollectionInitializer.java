package com.libreqr.config;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.stereotype.Component;

/**
 * Inicialización idempotente de la base de datos al arrancar.
 * <p>Comprueba la conexión con MongoDB y que existan las colecciones requeridas por esta versión de la aplicación; si falta alguna, la crea. No elimina colecciones ni índices existentes.</p>
 * <ul>
 *   <li>Comprueba la conexión con MongoDB y que existan las colecciones requeridas
 *       por esta versión de la aplicación; si falta alguna, la crea.</li>
 *   <li>Es idempotente: ejecutarlo varias veces no borra ni duplica datos.</li>
 *   <li>No elimina colecciones ni índices existentes (los cambios estructurales se
 *       gestionan mediante migraciones explícitas y documentadas).</li>
 *   <li>Si MongoDB no está disponible o la inicialización falla, registra el error y
 *       lanza una excepción, de modo que el backend no queda "listo".</li>
 * </ul>
 *
 * <p>Nota: la creación de índices se añadirá cuando se definan los documentos; por ahora
 * solo se garantiza la existencia de las colecciones (sin consultas ni mapeo).</p>
 */
@Component
public class MongoCollectionInitializer {

    private static final Logger log = LoggerFactory.getLogger(MongoCollectionInitializer.class);

    /** Colecciones requeridas por la aplicación según el diagrama de BD del documento. */
    private static final List<String> REQUIRED_COLLECTIONS = List.of(
            "users",
            "qr_codes",
            "campaigns",
            "campaign_qrs",
            "scan_events"
    );

    private final MongoTemplate mongoTemplate;

    public MongoCollectionInitializer(MongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    @EventListener(ApplicationReadyEvent.class)
    public void initialize() {
        try {
            var db = mongoTemplate.getDb();

            // Verifica la conexión con un ping (falla rápido si Mongo no está).
            db.runCommand(new org.bson.Document("ping", 1));

            for (String collection : REQUIRED_COLLECTIONS) {
                if (mongoTemplate.collectionExists(collection)) {
                    log.info("Colección ya presente: {}", collection);
                } else {
                    mongoTemplate.createCollection(collection);
                    log.info("Colección creada: {}", collection);
                }
            }

            log.info("MongoDB inicializado correctamente. Colecciones requeridas: {}",
                    REQUIRED_COLLECTIONS);
        } catch (Exception ex) {
            log.error("No se pudo inicializar MongoDB. El servicio NO está listo.", ex);
            throw new IllegalStateException("Inicialización de MongoDB fallida", ex);
        }
    }
}
