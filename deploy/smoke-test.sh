#!/usr/bin/env bash
# Teste de fumaça: confere, na própria VPS, se a API publicada responde conforme o contrato.
# Uso: deploy/smoke-test.sh [base-url]   (padrão: http://localhost:8080)
set -uo pipefail

BASE="${1:-http://localhost:8080}"
FAILS=0

check() { # descrição, comando que deve sair com 0
  if bash -c "$2" >/dev/null 2>&1; then echo "  ok   $1"; else echo "  FALHA $1"; FAILS=$((FAILS + 1)); fi
}

echo "Smoke test em $BASE"
check "saúde da aplicação (UP)"                     "curl -fsS -m 10 '$BASE/actuator/health' | grep -q UP"
check "busca pública devolve exames"                "curl -fsS -m 10 '$BASE/api/v1/exams/search?size=1' | grep -q '\"total\"'"
check "busca por nome popular (colesterol)"         "curl -fsS -m 10 '$BASE/api/v1/exams/search?query=colesterol' | grep -q 'Lipidograma'"
check "lista de bairros (RN38)"                     "curl -fsS -m 10 '$BASE/api/v1/neighborhoods' | grep -q 'Centro'"
check "exames em alta (RN30)"                       "curl -fsS -m 10 '$BASE/api/v1/exam-types/trending' | grep -q 'min_price'"
check "rota protegida exige token (RN24 → 401)"     "[ \"\$(curl -s -m 10 -o /dev/null -w '%{http_code}' '$BASE/api/v1/me')\" = 401 ]"
check "erro segue RFC 9457 (RNF07)"                 "curl -s -m 10 -H 'Accept: application/json' '$BASE/api/v1/exams/00000000-0000-0000-0000-000000000000' | grep -q 'EXAM_NOT_FOUND'"
# A primeira chamada gera o documento e pode levar mais de 10 s em VMs pequenas.
check "contrato OpenAPI publicado (RNF06)"          "curl -fsS -m 45 '$BASE/v3/api-docs' | grep -q '/api/v1/exams/search'"

if [ "$FAILS" -gt 0 ]; then echo "$FAILS verificação(ões) falharam" >&2; exit 1; fi
echo "Todas as verificações passaram"
