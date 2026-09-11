#!/bin/sh
set -eu

BACKUP_DIR="${BACKUP_DIR:-/backups}"
KEEP_DAYS="${BACKUP_KEEP_DAYS:-7}"
INTERVAL_SECONDS="${BACKUP_INTERVAL_SECONDS:-86400}"

mkdir -p "$BACKUP_DIR"

while true; do
	stamp=$(date -u +%Y%m%dT%H%M%SZ)
	file="$BACKUP_DIR/vida_conecta_${stamp}.sql.gz"
	echo "Backup $file"
	if pg_dump -h postgres -U "$POSTGRES_USER" -d "$POSTGRES_DB" | gzip > "$file"; then
		date -u +%s > "$BACKUP_DIR/last_success_epoch"
		find "$BACKUP_DIR" -name 'vida_conecta_*.sql.gz' -mtime +"$KEEP_DAYS" -delete
	else
		echo "Falha no pg_dump" >&2
		rm -f "$file"
	fi
	sleep "$INTERVAL_SECONDS"
done
