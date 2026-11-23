#!/usr/bin/env bash
# Arranca el backend en LOCAL con hot reload para desarrollo.
#
# Que hace:
#   1. Lee las credenciales del .env raiz (los secretos nunca van al codigo).
#   2. Libera el puerto de desarrollo parando el contenedor compose del backend
#      (si no, colisionaria con ./mvnw spring-boot:run y el proxy del frontend
#      apuntaria al contenedor viejo en lugar de a tu codigo).
#   3. Arranca ./mvnw spring-boot:run con MONGO_URI y SERVER_PORT correctos.
#
# Uso:  ./run-dev.sh          (Ctrl+C para parar)
# Para volver al contenedor:   docker compose up -d backend   (desde la raiz)
set -euo pipefail

cd "$(dirname "$0")"
ROOT="$(cd .. && pwd)"

if [[ ! -f "$ROOT/.env" ]]; then
    echo "Falta $ROOT/.env (copia .env.example)." >&2
    exit 1
fi

set -a
# shellcheck disable=SC1091
source "$ROOT/.env"
set +a

export MONGO_URI="mongodb://${MONGO_USER}:${MONGO_PASSWORD}@localhost:27017/${MONGO_DB}?authSource=${MONGO_DB}"
export SERVER_PORT="${SERVER_PORT:-8080}"

echo "[run-dev] parando el backend en Docker para liberar :${SERVER_PORT}"
docker compose -f "$ROOT/docker-compose.yml" stop backend >/dev/null 2>&1 || true

echo "[run-dev] backend local en http://localhost:${SERVER_PORT} (Mongo: ${MONGO_DB})"
exec ./mvnw spring-boot:run
