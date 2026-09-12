#!/usr/bin/env bash
# Redeploy via Docker Compose no EC2 (SSM). Os arquivos de runtime
# (compose, nginx, observability) já foram escritos neste mesmo comando.
# O .env da instância não é tocado.
#
# Puxa só a imagem da API; o `up -d` recria nginx/grafana/prometheus se o
# compose ou os volumes de config mudaram. Postgres permanece se nada mudou.
set -euo pipefail

IMAGE="${DEPLOY_IMAGE:?defina DEPLOY_IMAGE (ex.: usuario/vida-conecta-backend:1.1.1.20260912.120000)}"
COMPOSE_DIR="${DEPLOY_COMPOSE_DIR:?defina DEPLOY_COMPOSE_DIR (ex.: /home/ubuntu)}"
COMPOSE_FILE="docker-compose.prod.yml"
CONTAINER="vida-conecta-api"

cd "$COMPOSE_DIR"

if [ ! -f .env ]; then
	echo "Não encontrei $COMPOSE_DIR/.env — copie .env.prod.example pra .env e preencha antes do primeiro deploy." >&2
	exit 1
fi

if [ -f observability/postgres/backup-loop.sh ]; then
	chmod +x observability/postgres/backup-loop.sh
fi

export BACKEND_IMAGE="$IMAGE"

echo "==> Baixando $IMAGE..."
docker compose -f "$COMPOSE_FILE" pull api

echo "==> Aplicando o compose (API + nginx + observabilidade)..."
docker compose -f "$COMPOSE_FILE" up -d

echo "==> Aguardando health check..."
for _ in $(seq 1 40); do
	status=$(docker inspect --format '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "indisponível")
	if [ "$status" = "healthy" ]; then
		echo "Deploy concluído: '$CONTAINER' está healthy, rodando $IMAGE."
		docker compose -f "$COMPOSE_FILE" ps
		exit 0
	fi
	sleep 3
done

echo "O container subiu mas não ficou 'healthy' em 120s (status: $status). Últimas linhas do log:" >&2
docker compose -f "$COMPOSE_FILE" logs --tail 80 api >&2
exit 1
