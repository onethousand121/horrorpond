#!/usr/bin/env bash
# 매일 DB 백업 (setup-server.sh가 cron에 등록). 서버에 7일치를 남기고,
# .env 에 BACKUP_RCLONE_REMOTE(예: gdrive:horrorpond-backup)가 있으면 서버 밖으로도 복사한다.
set -euo pipefail
cd "$(dirname "$0")"
set -a; source .env; set +a

mkdir -p backups
FILE="backups/horrorpond-$(date +%Y%m%d-%H%M).dump"
docker compose exec -T postgres pg_dump -U horrorpond -Fc horrorpond > "$FILE"
find backups -name 'horrorpond-*.dump' -mtime +7 -delete

if [ -n "${BACKUP_RCLONE_REMOTE:-}" ] && command -v rclone > /dev/null; then
  rclone copy "$FILE" "$BACKUP_RCLONE_REMOTE"
  rclone delete --min-age 30d "$BACKUP_RCLONE_REMOTE"
fi
echo "backup: $FILE ($(du -h "$FILE" | cut -f1))"
