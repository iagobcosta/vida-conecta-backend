#!/usr/bin/env bash
# Redeploy via Docker Compose no EC2 (SSM). Os arquivos de runtime
# (compose, nginx, observability) já foram escritos neste mesmo comando.
# O .env da instância não é tocado, exceto BACKEND_IMAGE (tag publicada).
#
# Nginx só processa o template no start; Grafana só relê provisioning no
# start. Por isso nginx/grafana/prometheus são recriados a cada deploy.
# Postgres permanece se o serviço não mudou.
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

if grep -q '^BACKEND_IMAGE=' .env; then
	sed -i "s|^BACKEND_IMAGE=.*|BACKEND_IMAGE=${IMAGE}|" .env
else
	printf '\nBACKEND_IMAGE=%s\n' "$IMAGE" >> .env
fi

export BACKEND_IMAGE="$IMAGE"

echo "==> Baixando $IMAGE..."
docker compose -f "$COMPOSE_FILE" pull api

echo "==> Recriando nginx, Grafana e Prometheus com o config do repositório..."
docker compose -f "$COMPOSE_FILE" up -d --force-recreate --no-deps nginx grafana prometheus

echo "==> Aplicando o compose (API e demais serviços)..."
docker compose -f "$COMPOSE_FILE" up -d --remove-orphans

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
