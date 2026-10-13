#!/usr/bin/env bash
# Publica o back-end na VPS: constrói a imagem aqui, envia pronta e sobe os containers.
# Uso: deploy/deploy.sh <usuario>@<ip> [caminho-da-chave]
set -euo pipefail

TARGET="${1:?uso: deploy.sh usuario@ip [chave]}"
KEY="${2:-}"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
[[ -n "$KEY" ]] && SSH_OPTS+=(-i "$KEY" -o IdentitiesOnly=yes)
REMOTE_DIR="/opt/clinics-backend"
IMAGE="clinics-api:latest"

ssh_run() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }

echo "==> Construindo a imagem"
docker build -t "$IMAGE" .

echo "==> Preparando a VPS"
ssh_run "command -v docker >/dev/null || { echo 'Docker não instalado na VPS' >&2; exit 1; }"
ssh_run "sudo mkdir -p $REMOTE_DIR && sudo chown \$USER $REMOTE_DIR"

echo "==> Enviando a imagem e o compose"
docker save "$IMAGE" | gzip | ssh_run 'gunzip | sudo docker load'
scp "${SSH_OPTS[@]}" compose.prod.yaml "$TARGET:$REMOTE_DIR/compose.prod.yaml"

echo "==> Configurando segredos (só na primeira vez)"
ssh_run "cd $REMOTE_DIR && [ -f .env ] || { printf 'DB_PASSWORD=%s\nJWT_SECRET=%s\n' \"\$(openssl rand -hex 16)\" \"\$(openssl rand -hex 32)\" > .env; chmod 600 .env; }"

echo "==> Subindo os containers"
ssh_run "cd $REMOTE_DIR && sudo docker compose -f compose.prod.yaml up -d"

echo "==> Aguardando a API"
for _ in $(seq 1 40); do
  if ssh_run 'curl -fs http://localhost:8080/actuator/health' | grep -q UP; then
    echo "API no ar na VPS (porta 8080)"; exit 0
  fi
  sleep 5
done
echo "A API não respondeu a tempo. Veja: sudo docker compose -f $REMOTE_DIR/compose.prod.yaml logs api" >&2
exit 1
