#!/usr/bin/env bash
# Gera o app web (PWA) e coloca em src/main/resources/static/app, para a API servir em /app/.
# Uso: deploy/build-web.sh [pasta-do-projeto-flutter] [url-da-api]
#   padrão: ../clinics-mobile/clinics  e  https://clinics.marciodev.com
set -euo pipefail
cd "$(dirname "$0")/.."

FLUTTER_DIR="${1:-../clinics-mobile/clinics}"
API_URL="${2:-https://clinics.marciodev.com}"
DEST="src/main/resources/static/app"

( cd "$FLUTTER_DIR" && flutter pub get && flutter build web --release --base-href /app/ \
    --no-wasm-dry-run --dart-define=API_BASE_URL="$API_URL" )

rm -rf "$DEST" && mkdir -p "$DEST"
cp -r "$FLUTTER_DIR/build/web/." "$DEST/"
echo "App web copiado para $DEST ($(du -sh "$DEST" | cut -f1))"
