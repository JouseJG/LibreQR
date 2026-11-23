/**
 * Configuracion de la API.
 *
 * El frontend siempre llama a rutas relativas (`/api/...`): en dev el proxy de
 * `ng serve` (`proxy.conf.json`) redirige al backend (`:8090`) y en produccion
 * nginx sirve frontend y API bajo el mismo origen. Asi la cookie de sesion
 * (`libreqr_sid`, SameSite=Lax) funciona sin CORS ni dominio explicito.
 */
export const API_BASE_URL = '/api';

/** Ventana por defecto de metricas cuando el backend no la indica (REQ-MET-03). */
export const METRICS_DEFAULT_WINDOW_DAYS = 30;
