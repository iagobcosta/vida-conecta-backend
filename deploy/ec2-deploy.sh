#!/usr/bin/env bash
# Redeploy do container da API no EC2. Roda dentro da instância (via SSM
# Run Command), nunca no runner do GitHub Actions.
#
# Clona a configuração de runtime do container atual (env vars, porta, rede,
# política de restart) antes de recriá-lo: assim o pipeline nunca precisa
# conhecer segredo nenhum nem a topologia de rede da instância — só troca a
# imagem, preservando tudo o que já estava configurado manualmente.
set -euo pipefail

IMAGE="${DEPLOY_IMAGE:?defina DEPLOY_IMAGE (ex.: usuario/vida-conecta-backend:latest)}"
CONTAINER="${DEPLOY_CONTAINER:?defina DEPLOY_CONTAINER (nome do container da API)}"

if ! docker inspect "$CONTAINER" >/dev/null 2>&1; then
	echo "Container '$CONTAINER' não encontrado nesta instância; abortando." >&2
	exit 1
fi

echo "==> Clonando configuração de runtime de '$CONTAINER'..."

# .Config.Env do container traz TUDO: tanto as env vars que você passou com
# -e quanto as que a própria imagem antiga já embutia (PATH, JAVA_HOME etc.).
# Clonar isso tudo e aplicar numa imagem nova sobrescreveria os defaults dela
# (ex.: um PATH sem o java) com os defaults da imagem ANTIGA. Por isso
# comparamos com os defaults da imagem antiga e só clonamos o que sobrou —
# ou seja, só o que foi de fato passado em tempo de execução.
#
# "docker inspect --format" acrescenta uma quebra de linha própria no final,
# além da que o template já produz — o "grep -v '^$'" descarta essa linha
# vazia antes do mapfile (em vez de "local -n"/nameref, que é recurso do
# bash 4.3+ e não existe em toda distro).
IMAGEM_ANTIGA=$(docker inspect "$CONTAINER" --format '{{.Config.Image}}')
mapfile -t ENVS_DO_CONTAINER < <(docker inspect "$CONTAINER" --format '{{range .Config.Env}}{{.}}{{"\n"}}{{end}}' | grep -v '^$')
mapfile -t ENVS_DA_IMAGEM_ANTIGA < <(docker inspect "$IMAGEM_ANTIGA" --format '{{range .Config.Env}}{{.}}{{"\n"}}{{end}}' | grep -v '^$')

ENV_ARGS=()
for env in "${ENVS_DO_CONTAINER[@]}"; do
	era_default_da_imagem=false
	for default in "${ENVS_DA_IMAGEM_ANTIGA[@]}"; do
		if [ "$env" = "$default" ]; then
			era_default_da_imagem=true
			break
		fi
	done
	[ "$era_default_da_imagem" = true ] || ENV_ARGS+=("--env=$env")
done

# "--flag=valor" num token só: cada linha do mapfile vira um elemento de array,
# então flag e valor precisam estar juntos, sem espaço, ou o Docker CLI recebe
# "--publish 8080:8080/tcp" como uma flag desconhecida em vez de duas.
mapfile -t PORT_ARGS < <(docker inspect "$CONTAINER" --format '{{range $porta, $conf := .HostConfig.PortBindings}}{{range $conf}}--publish={{.HostPort}}:{{$porta}}{{"\n"}}{{end}}{{end}}' | grep -v '^$')
NETWORK_MODE=$(docker inspect "$CONTAINER" --format '{{.HostConfig.NetworkMode}}')
RESTART_POLICY=$(docker inspect "$CONTAINER" --format '{{.HostConfig.RestartPolicy.Name}}')
RESTART_POLICY="${RESTART_POLICY:-unless-stopped}"

echo "==> Baixando $IMAGE..."
docker pull "$IMAGE"

echo "==> Parando e removendo '$CONTAINER'..."
docker stop "$CONTAINER" >/dev/null
docker rm "$CONTAINER" >/dev/null

echo "==> Subindo o novo container..."
docker run -d \
	--name "$CONTAINER" \
	--network "$NETWORK_MODE" \
	--restart "$RESTART_POLICY" \
	"${PORT_ARGS[@]}" \
	"${ENV_ARGS[@]}" \
	"$IMAGE" >/dev/null

echo "==> Aguardando health check..."
for _ in $(seq 1 30); do
	if docker exec "$CONTAINER" wget -qO- http://localhost:8080/actuator/health 2>/dev/null | grep -q '"status":"UP"'; then
		echo "Deploy concluído: '$CONTAINER' está UP, rodando $IMAGE."
		exit 0
	fi
	sleep 2
done

echo "O container subiu mas não respondeu UP em 60s. Últimas linhas do log:" >&2
docker logs --tail 50 "$CONTAINER" >&2
exit 1
