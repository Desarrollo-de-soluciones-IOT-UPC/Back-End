#!/usr/bin/env bash
# ---------------------------------------------------------------------------
# Respaldo diario de MySQL.
#
# La contrapartida de auto-hospedar la base de datos es que los backups corren
# por nuestra cuenta. Esto vuelca la BD a deploy/backups/ y conserva 14 días.
#
# Instalar en cron (una sola vez):
#   chmod +x backup.sh
#   (crontab -l 2>/dev/null; echo "15 4 * * * $(pwd)/backup.sh >> $(pwd)/backups/backup.log 2>&1") | crontab -
# ---------------------------------------------------------------------------
set -euo pipefail

cd "$(dirname "$0")"
set -a; source .env; set +a

RETENCION_DIAS=14
DESTINO="./backups"
STAMP=$(date +%Y-%m-%d_%H%M)
ARCHIVO="emsafe_db_${STAMP}.sql.gz"

mkdir -p "$DESTINO"

echo "[$(date '+%F %T')] respaldando ${MYSQL_DATABASE}..."

docker compose -f docker-compose.prod.yml exec -T db \
  mysqldump -u root -p"${MYSQL_ROOT_PASSWORD}" \
    --single-transaction --quick --routines --triggers \
    "${MYSQL_DATABASE}" | gzip > "${DESTINO}/${ARCHIVO}"

# Si el volcado salió vacío o corrupto, mejor fallar ruidosamente que guardar basura.
if [ ! -s "${DESTINO}/${ARCHIVO}" ]; then
  echo "[$(date '+%F %T')] ERROR: el respaldo salió vacío" >&2
  rm -f "${DESTINO}/${ARCHIVO}"
  exit 1
fi

find "$DESTINO" -name 'emsafe_db_*.sql.gz' -mtime "+${RETENCION_DIAS}" -delete

echo "[$(date '+%F %T')] OK -> ${ARCHIVO} ($(du -h "${DESTINO}/${ARCHIVO}" | cut -f1))"
