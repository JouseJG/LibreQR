#!/usr/bin/env bash
# LibreQR — pruebas en local.
# Levanta el Mongo del docker compose (si hace falta) y ejecuta la suite completa
# (surefire + failsafe: *IT incluidos) contra él.
#
# Uso:
#   ./scripts/test-local.sh              # suite completa
#   ./scripts/test-local.sh QrApiIT      # una clase concreta (-Dtest=X)
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
cd "$ROOT"

# Cargar .env (puertos/credenciales de desarrollo).
if [ -f .env ]; then
  set -a; . ./.env; set +a
fi
MONGO_PORT="${MONGO_PORT:-27017}"

echo "==> Asegurando MongoDB en localhost:${MONGO_PORT} (docker compose)"
docker compose up -d mongodb

echo "==> Esperando a que Mongo responda al healthcheck"
for _ in $(seq 1 30); do
  status=$(docker inspect --format '{{.State.Health.Status}}' libreqr-mongo 2>/dev/null || echo "starting")
  [ "$status" = "healthy" ] && break
  sleep 2
done
[ "${status:-}" = "healthy" ] || { echo "Mongo no se puso healthy"; exit 1; }

# Credenciales de desarrollo (ver .env / mongo init script).
export MONGO_URI="mongodb://${MONGO_USER:-libreqr}:${MONGO_PASSWORD:-devlibreqrpass}@localhost:${MONGO_PORT}/${MONGO_DB:-libreqr}?authSource=${MONGO_DB:-libreqr}"

echo "==> Ejecutando tests (MONGO_URI=localhost:${MONGO_PORT})"
cd "$ROOT/backend"
if [ $# -gt 0 ]; then
  ./mvnw -B -Dtest="$1" test
else
  ./mvnw -B clean verify
fi

echo "==> Listo. Nota: docker compose stop backend  # si quieres parar el backend local."
