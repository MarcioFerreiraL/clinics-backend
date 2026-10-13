# 09 · Deploy e pipeline (CI/CD)

## Visão geral

```
git push (develop) ──► GitHub Actions ──► testes ──► build da imagem ──► deploy por SSH ──► VPS (Docker)
```

- **CI** (a cada push e pull request): roda `./mvnw verify` (154+ testes, com PostgreSQL real via Testcontainers) e confere se a imagem Docker compila.
- **CD** (só push na `develop`, e só se `DEPLOY_ENABLED=true`): conecta na VPS por SSH, envia o código e sobe os containers com `docker compose`.

## Arquivos

| Arquivo | Função |
|---|---|
| `Dockerfile` | Imagem em duas etapas: compila com JDK 25 e roda só com o JRE |
| `compose.prod.yaml` | PostgreSQL + API, com volumes (banco e avatares), reinício automático e healthcheck |
| `.env.example` | Modelo dos segredos (`DB_PASSWORD`, `JWT_SECRET`); o `.env` real nunca vai para o Git |
| `deploy/deploy.sh` | Instala o Docker se faltar, envia o código (rsync), cria o `.env` com segredos aleatórios na primeira vez e sobe tudo |
| `.github/workflows/ci-cd.yml` | O pipeline |

## Configuração única no GitHub

Em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `VPS_HOST` | IP da VPS |
| Secret | `VPS_USER` | usuário SSH (`ubuntu` ou `opc` na Oracle) |
| Secret | `VPS_SSH_KEY` | conteúdo da **chave privada** autorizada na VPS |
| Variable | `DEPLOY_ENABLED` | `true` para ligar o deploy |

## Rede na Oracle Cloud

A API escuta na porta 8080. É preciso liberar nos dois lugares:

1. **Console Oracle** → rede virtual (VCN) → Security List → regra de entrada TCP na porta `8080`.
2. **Dentro da VPS** (imagens Ubuntu da Oracle bloqueiam por padrão):
   `sudo iptables -I INPUT -p tcp --dport 8080 -j ACCEPT && sudo netfilter-persistent save`

## Deploy manual

```bash
deploy/deploy.sh ubuntu@204.216.160.34 caminho/da/chave
```

## Operação

```bash
ssh ubuntu@IP
cd /opt/clinics-backend
sudo docker compose -f compose.prod.yaml logs -f api    # logs
sudo docker compose -f compose.prod.yaml ps             # estado
sudo docker compose -f compose.prod.yaml restart api    # reiniciar
```

O banco e as fotos de perfil ficam em volumes Docker e sobrevivem a novos deploys.

## Segurança e próximos passos

- A API roda em HTTP. Para HTTPS é preciso um domínio (ex.: Caddy ou Nginx com Let's Encrypt) e trocar a URL base do app.
- O Swagger e o `/v3/api-docs` estão públicos; considere desligá-los em produção (`springdoc.api-docs.enabled=false`).
- O banco não expõe porta para fora da VPS.
