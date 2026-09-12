#!/usr/bin/env bash
# Redeploy do serviço "api" via Docker Compose. Roda dentro do EC2 (via SSM
# Run Command), nunca no runner do GitHub Actions. O docker-compose.prod.yml
# usado aqui já foi escrito nesse mesmo comando SSM, sempre a versão do
# commit publicado — não existe um arquivo desatualizado só na instância.
#
# Só o serviço "api" é baixado/recriado. Postgres, Nginx e a stack de
# observabilidade (Prometheus, Grafana, exporter, backup) não são tocados:
# o Compose só recria um serviço quando a configuração dele muda, e a única
# coisa que muda a cada deploy é a imagem da API (via BACKEND_IMAGE).
set -euo pipefail

IMAGE="${DEPLOY_IMAGE:?defina DEPLOY_IMAGE (ex.: usuario/vida-conecta-backend:latest)}"
COMPOSE_DIR="${DEPLOY_COMPOSE_DIR:?defina DEPLOY_COMPOSE_DIR (ex.: /home/ubuntu)}"
COMPOSE_FILE="docker-compose.prod.yml"
CONTAINER="vida-conecta-api"

cd "$COMPOSE_DIR"

if [ ! -f .env ]; then
	echo "Não encontrei $COMPOSE_DIR/.env — copie .env.prod.example pra .env e preencha antes do primeiro deploy." >&2
	exit 1
fi

export BACKEND_IMAGE="$IMAGE"

echo "==> Baixando $IMAGE..."
docker compose -f "$COMPOSE_FILE" pull api

echo "==> Subindo o serviço 'api'..."
docker compose -f "$COMPOSE_FILE" up -d api

echo "==> Aguardando health check..."
for _ in $(seq 1 30); do
	status=$(docker inspect --format '{{.State.Health.Status}}' "$CONTAINER" 2>/dev/null || echo "indisponível")
	if [ "$status" = "healthy" ]; then
		echo "Deploy concluído: '$CONTAINER' está healthy, rodando $IMAGE."
		exit 0
	fi
	sleep 2
done

echo "O container subiu mas não ficou 'healthy' em 60s (status: $status). Últimas linhas do log:" >&2
docker compose -f "$COMPOSE_FILE" logs --tail 50 api >&2
exit 1
