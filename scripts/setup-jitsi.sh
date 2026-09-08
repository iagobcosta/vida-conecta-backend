#!/usr/bin/env bash
# Sobe o Jitsi Meet self-hosted para o Vida Conecta (dev/demo local).
set -euo pipefail

ROOT="$(cd "$(dirname "$0")/.." && pwd)"
JITSI_DIR="$ROOT/jitsi"
CFG_DIR="$ROOT/jitsi-cfg"
TAG="${JITSI_RELEASE_TAG:-stable-11146-2}"

if [[ ! -f "$JITSI_DIR/docker-compose.yml" ]]; then
  echo ">> Baixando docker-jitsi-meet ($TAG)..."
  mkdir -p "$JITSI_DIR"
  curl -sL "https://github.com/jitsi/docker-jitsi-meet/archive/refs/tags/${TAG}.tar.gz" \
    | tar -xz -C "$JITSI_DIR" --strip-components=1
fi

cd "$JITSI_DIR"

if [[ ! -f .env ]]; then
  echo ">> Criando .env local..."
  cp env.example .env
  ./gen-passwords.sh

  HOST_IP="$(ipconfig getifaddr en0 2>/dev/null || ipconfig getifaddr en1 2>/dev/null || echo 127.0.0.1)"

  # macOS sed needs '' after -i
  if [[ "$(uname)" == "Darwin" ]]; then
    sed -i '' "s|CONFIG=~/.jitsi-meet-cfg|CONFIG=../jitsi-cfg|" .env
    sed -i '' "s|#PUBLIC_URL=https://meet.example.com:\${HTTPS_PORT}|PUBLIC_URL=https://localhost:8443|" .env
  else
    sed -i "s|CONFIG=~/.jitsi-meet-cfg|CONFIG=../jitsi-cfg|" .env
    sed -i "s|#PUBLIC_URL=https://meet.example.com:\${HTTPS_PORT}|PUBLIC_URL=https://localhost:8443|" .env
  fi

  cat >> .env <<EOF

# --- Vida Conecta local ---
HTTP_PORT=8000
HTTPS_PORT=8443
DISABLE_HTTPS=0
ENABLE_LETSENCRYPT=0
ENABLE_AUTH=0
BOSH_RELATIVE=1
ENABLE_XMPP_WEBSOCKET=0
TZ=America/Fortaleza
JVB_COLIBRI_PORT=8082
JVB_ADVERTISE_IPS=${HOST_IP}
CSP_HEADER=frame-ancestors http://localhost:5173 http://localhost:5174 http://127.0.0.1:5173 http://127.0.0.1:5174
CORS_HEADER_ACCESS_CONTROL_ALLOW_ORIGIN=*
EOF
fi

mkdir -p "$CFG_DIR"/{web/nginx-custom,transcripts,prosody/config,prosody/prosody-plugins-custom,jicofo,jvb}

echo ">> Subindo containers Jitsi..."
docker compose up -d

echo
echo "Jitsi web (HTTPS): https://localhost:8443"
echo "1) Abra https://localhost:8443 no navegador e aceite o certificado autoassinado"
echo "2) No frontend: VITE_JITSI_DOMAIN=localhost:8443 e reinicie o Vite"
echo "API Spring: http://localhost:8080 (JVB Colibri usa 8082)"
