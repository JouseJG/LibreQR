# LibreQR

**LibreQR** es una herramienta de *software libre* para **crear, gestionar y medir códigos
QR**, de forma individual o agrupados en campañas. Es autoalojada (self-hosted): tus QRs,
tus campañas y tus métricas viven en tu propia infraestructura, sin dependencia de ningún
proveedor externo ni cuotas.

- **Crea** códigos QR personalizados (colores, forma de los puntos, esquinas y logo) y
  descárgalos como PNG o SVG.
- **Organiza** los códigos en campañas para medir su rendimiento global.
- **Mide** escaneos, visitantes únicos y su desglose por dispositivo y país (estimación
  aproximada, sin inventar datos), con comparativa entre periodos.
- **Redirige** de forma estable: los QR dinámicos guardan un enlace corto
  (`/r/{slug}`) que puedes reapuntar sin volver a imprimir el código.

## Índice

- [Características](#características)
- [Stack técnico](#stack-técnico)
- [Arquitectura](#arquitectura)
- [Política del proyecto](#política-del-proyecto)
- [Requisitos](#requisitos)
- [Instalación](#instalación)
- [Desarrollo](#desarrollo)
- [Pruebas](#pruebas)
- [Configuración y puertos](#configuración-y-puertos)
- [Licencia](#licencia)

## Características

- Registro e inicio de sesión por **cookie de sesión** (`libreqr_sid`, HttpOnly +
  SameSite=Lax); sin tokens visibles.
- CRUD de **códigos QR** y **campañas**, con buscador, filtros, orden y paginación en
  servidor.
- **Editor de diseño** con previsualización en vivo; exportación **PNG** y **SVG**.
- **Panel de métricas** (dashboard) y pestaña de estadísticas por campaña/QR con rango de
  fechas configurable y **exportación CSV**.
- Detección aproximada de **bots** (excluidos del recuento) y clasificación por
  dispositivo; **usuarios y datos totalmente aislados** por propietario.
- Borrado **lógico** de recursos; un QR solo pertenece a una campaña; el estado de la
  campaña se propaga a sus QR.

## Stack técnico

| Componente | Tecnología |
| ---------- | ---------- |
| Frontend | Angular 22 (standalone + señales) |
| Backend | Spring Boot 4.1 sobre Java 21 |
| Base de datos | MongoDB 7 |
| Despliegue | Docker Compose (MongoDB + backend) |

> Las imágenes de los QR se **generan en el cliente** con `qr-code-styling` y se descargan
> directamente; el backend no almacena binarios de imagen.

## Arquitectura

```
┌──────────────┐     /api, /r      ┌──────────────────┐   Spring Data   ┌──────────┐
│   Frontend   │ ────────────────► │     Backend      │ ──────────────► │ MongoDB  │
│  (Angular)   │  proxy dev /      │ (Spring Boot)    │                 │          │
│  estático    │  reverse proxy    │  API REST + /r/*  │                 │          │
└──────────────┘                   └──────────────────┘                 └──────────┘
```

- **Frontend**: SPA Angular. En desarrollo usa un proxy (`frontend/proxy.conf.json`) que
  reenvía `/api` y `/r` al backend en `:8080`. En producción se compila a estáticos y un
  reverse proxy (nginx, Caddy, Apache…) enruta `/api` y `/r` al backend.
- **Backend**: API REST bajo `/api/**` + endpoint público de redirección `/r/{slug}`.
  Autenticación por sesión; modo desarrollo que acepta el header `X-User-Id` para tests E2E.
- **Base de datos**: MongoDB con usuario de aplicación (rol `readWrite`) creado por un
  *init-script* idempotente.

### Estructura del repositorio

```
.
├── backend/            # API Spring Boot (mvnw, Dockerfile, run-dev.sh)
├── frontend/           # SPA Angular (proxy.conf.json)
├── docker-compose.yml  # MongoDB + backend
├── scripts/            # deploy.sh, test-local.sh
├── doc/                # Documentación técnica y registro del proyecto
└── .env.example        # Plantilla de variables (copiar a .env)
```

## Política del proyecto

LibreQR es software libre y quiere seguir siéndolo. Estas son sus reglas de diseño:

- **Copyleft (GPLv3).** Cualquier distribución o trabajo derivado debe publicarse bajo la
  misma licencia. Usa el archivo [`LICENSE`](./LICENSE) tal cual.
- **Self-hosted y sin lock-in.** Sin servicios de pago ni proveedores externos obligatorios:
  solo Angular, Spring Boot y MongoDB. Todo corre en tu máquina.
- **Privacidad por diseño.** No hay balizadores ni rastreadores de terceros. Los visitantes
  únicos se estiman con un **hash de una cookie** (no se guarda el identificador en claro);
  la ubicación es **aproximada** (por idioma del navegador), sin GeoIP.
- **No inventar datos.** Cuando una métrica no está disponible se muestra vacía o a cero,
  nunca simulada (p. ej. el país queda `unknown` si no se puede deducir).
- **Sin dependencias comerciales para funciones base.** Se han descartado librerías de
  pago/marca de agua (p. ej. `qr-border-plugin`) por incompatibles con un proyecto libre.
- **Registro y plan vivos.** Cada hito se documenta en el historial de
  [`doc/README.md`](./doc/README.md) y el plan de trabajo se mantiene en
  [`doc/PLAN.md`](./doc/PLAN.md). La documentación técnica completa está en
  `doc/LibreQRDocuDisenyoTecnico.pdf`.

## Requisitos

- **Docker** y **Docker Compose** v2 (para desplegar).
- **JDK 21** y **Node.js 20+** (probado con 22) solo si vas a compilar/desarrollar en local.
  El backend trae Maven vía *wrapper* (`backend/mvnw`); no hace falta instalar Maven.

## Instalación

### Opción A — Docker (recomendado)

```bash
# 1. Crea tu .env a partir de la plantilla y edita las contraseñas
cp .env.example .env
$EDITOR .env

# 2. Construye la imagen del backend y levanta MongoDB + backend
./scripts/deploy.sh
```

`deploy.sh` espera a que el backend quede *healthy*, imprime la URL y deja un par de
`curl` de prueba (registro y login). El backend queda en `http://localhost:8080`.

**Frontend.** El `docker-compose.yml` solo conteneriza backend + MongoDB. Sirve la SPA
compilándola a estáticos:

```bash
cd frontend
npm install
npm run build          # genera frontend/dist/
# Sirve frontend/dist/ con tu servidor web favorito y enruta /api y /r al backend (:8080).
```

Gestiona el stack con:

```bash
./scripts/deploy.sh          # build + up + healthcheck
./scripts/deploy.sh down     # parar (los datos sobreviven en el volumen)
./scripts/deploy.sh reset    # down -v  (BORRA la base de datos de desarrollo)
```

### Opción B — Entorno de desarrollo

Levanta solo la base de datos en Docker y ejecuta backend y frontend en local (con recarga
en caliente):

```bash
# 1. Base de datos
docker compose up -d mongodb

# 2. Backend en local (lee .env, libera el puerto y arranca spring-boot:run)
./backend/run-dev.sh

# 3. Frontend en otro terminal (proxy -> http://localhost:8080)
cd frontend
npm install
npm start              # http://localhost:4200
```

## Pruebas

```bash
# Backend (surefire + failsafe, IT incluidos) contra el Mongo del compose
./scripts/test-local.sh            # suite completa
./scripts/test-local.sh QrApiIT    # una clase concreta

# Frontend
cd frontend && npm test
```

## Configuración y puertos

Toda la configuración sensible vive en **`.env`** (leído por `docker-compose` y por los
scripts). **Nunca subas el `.env` real** al repositorio; usa `.env.example` como plantilla.

| Variable | Descripción | Por defecto |
| -------- | ----------- | ----------- |
| `MONGO_INITDB_ROOT_USERNAME` / `..._PASSWORD` | Credenciales *root* de MongoDB | — |
| `MONGO_DB` | Base de datos de la aplicación | `libreqr` |
| `MONGO_USER` / `MONGO_PASSWORD` | Usuario de aplicación (`readWrite`) | `libreqr` / — |
| `SERVER_PORT` | Puerto expuesto del backend en el host | `8080` |

| Servicio | URL (desarrollo) |
| -------- | ---------------- |
| Frontend (dev) | http://localhost:4200 |
| Backend API | http://localhost:8080/api |
| Redirección QR | http://localhost:8080/r/{slug} |
| Health | http://localhost:8080/actuator/health |

Rutas de la API: `/api/auth/{register,login,logout}`, `/api/qr/**`, `/api/campaigns/**`,
`/api/stats/**`, `/api/qr/{id}/metrics` y `/api/qr/{id}/stats`.

## Licencia

Distribuido bajo **GNU GPL v3**. Ver [`LICENSE`](./LICENSE). Puedes usarlo, estudiarlo,
modificarlo y redistribuirlo (con las condiciones del copyleft).
