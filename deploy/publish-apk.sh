#!/usr/bin/env bash
# Publica um APK no site (/download/clinics.apk) sem precisar de novo deploy.
# Uso: deploy/publish-apk.sh <usuario>@<ip> <chave|-> <caminho/do/app.apk>
set -euo pipefail

TARGET="${1:?uso: publish-apk.sh usuario@ip chave|- app.apk}"
KEY="${2:?informe a chave SSH (ou - para usar a padrão)}"
APK="${3:?informe o caminho do APK}"
REMOTE_DIR="/opt/clinics-backend/downloads"

SSH_OPTS=(-o StrictHostKeyChecking=accept-new)
[[ "$KEY" != "-" ]] && SSH_OPTS+=(-i "$KEY" -o IdentitiesOnly=yes)

ssh "${SSH_OPTS[@]}" "$TARGET" "mkdir -p $REMOTE_DIR"
scp "${SSH_OPTS[@]}" "$APK" "$TARGET:$REMOTE_DIR/clinics.apk.new"
ssh "${SSH_OPTS[@]}" "$TARGET" "chmod 644 $REMOTE_DIR/clinics.apk.new && mv $REMOTE_DIR/clinics.apk.new $REMOTE_DIR/clinics.apk"
echo "APK publicado: $(du -h "$APK" | cut -f1) em $REMOTE_DIR/clinics.apk"
