#!/usr/bin/env bash
# LibreQR — despliegue local con Docker Compose.
# Construye la imagen del backend y levanta Mongo + backend con volumen persistente.
# Deja los servicios arriba y comprueba la salud.
#
# Uso:
#   ./scripts/deploy.sh            # build + up + healthcheck
#   ./scripts/deploy.sh down       # parar (los datos sobreviven)
#   ./scripts/deploy.sh reset      # down -v (BORRA la base de datos de desarrollo)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

if [ ! -f .env ]; then
  echo "No existe .env; copiando de .env.example"
  cp .env.example .env
  echo "=> Edita .env (contrasenas) antes del primer deploy."
fi

case "${1:-up}" in
  down)
    docker compose down
    echo "Servicios parados (datos conservados)."
    exit 0
    ;;
  reset)
    read -rp "Esto BORRA la base de datos de desarrollo (volumen mongo-data). Continuar? [y/N] " ans
    [ "${ans:-N}" = "y" ] || { echo "Cancelado."; exit 1; }
    docker compose down -v
    echo "Servicios parados y volumen borrado."
    exit 0
    ;;
  up) ;;
  *) echo "Uso: $0 [up|down|reset]"; exit 1 ;;
esac

echo "==> Construyendo imagen del backend"
docker compose build backend

echo "==> Levantando servicios (recrea contenedores antiguos)"
docker compose up -d --force-recreate

echo "==> Esperando a que el backend quede healthy"
for _ in $(seq 1 40); do
  status=$(docker inspect --format '{{.State.Health.Status}}' libreqr-back 2>/dev/null || echo "starting")
  [ "$status" = "healthy" ] && break
  sleep 3
done

if [ "${status:-}" != "healthy" ]; then
  echo "El backend NO quedo healthy. Logs:"
  docker compose logs --tail=50 backend
  exit 1
fi

# Cargar puerto de host desde .env
if [ -f .env ]; then set -a; . ./.env; set +a; fi
PORT="${SERVER_PORT:-8080}"

echo "==> Comprobacion final"
curl -fsS "http://localhost:${PORT}/actuator/health"; echo
echo "Backend:  http://localhost:${PORT}"
echo "Mongo:    mongodb://localhost:${MONGO_PORT:-27017} (libreqr/libreqr)"
echo
echo "Smoke rapido:"
echo "  curl -s -X POST http://localhost:${PORT}/api/auth/register -H 'Content-Type: application/json' -d '{\"email\":\"tú@correo.com\",\"name\":\"Tú\",\"password\":\"password123\"}'"
echo "  curl -s -i -X POST http://localhost:${PORT}/api/auth/login -H 'Content-Type: application/json' -d '{\"email\":\"tú@correo.com\",\"password\":\"password123\"}'"
