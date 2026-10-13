#!/usr/bin/env bash
# Publica o back-end na VPS: envia o código, instala o Docker se preciso e sobe os containers.
# Uso: deploy/deploy.sh <usuario>@<ip> [caminho-da-chave]
set -euo pipefail

TARGET="${1:?uso: deploy.sh usuario@ip [chave]}"
KEY="${2:-}"
SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
[[ -n "$KEY" ]] && SSH_OPTS+=(-i "$KEY" -o IdentitiesOnly=yes)
REMOTE_DIR="/opt/clinics-backend"

ssh_run() { ssh "${SSH_OPTS[@]}" "$TARGET" "$@"; }

echo "==> Preparando a VPS (Docker)"
ssh_run 'command -v docker >/dev/null || (curl -fsSL https://get.docker.com | sudo sh)'
ssh_run "sudo usermod -aG docker \$USER || true; sudo mkdir -p $REMOTE_DIR && sudo chown \$USER $REMOTE_DIR"

echo "==> Enviando o código"
rsync -az --delete -e "ssh ${SSH_OPTS[*]}" \
  --exclude target --exclude data --exclude .git --exclude .env --exclude .idea \
  ./ "$TARGET:$REMOTE_DIR/"

echo "==> Configurando segredos (só na primeira vez)"
ssh_run "cd $REMOTE_DIR && [ -f .env ] || { printf 'DB_PASSWORD=%s\nJWT_SECRET=%s\n' \"\$(openssl rand -hex 16)\" \"\$(openssl rand -hex 32)\" > .env; chmod 600 .env; }"

echo "==> Subindo os containers"
ssh_run "cd $REMOTE_DIR && sudo docker compose -f compose.prod.yaml up -d --build"

echo "==> Aguardando a API"
for i in $(seq 1 40); do
  if ssh_run 'curl -fs http://localhost:8080/actuator/health' | grep -q UP; then
    echo "API no ar: http://${TARGET#*@}:8080/swagger-ui.html"; exit 0
  fi
  sleep 5
done
echo "A API não respondeu a tempo. Veja: sudo docker compose -f $REMOTE_DIR/compose.prod.yaml logs api" >&2
exit 1
