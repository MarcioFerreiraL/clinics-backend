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
| `compose.yaml` | Desenvolvimento: PostgreSQL + API (construída pelo `Dockerfile`) com um comando |
| `compose.prod.yaml` | Produção: PostgreSQL + API (imagem pronta), com volumes (banco e avatares), reinício automático e healthcheck |
| `.env.example` | Modelo dos segredos (`DB_PASSWORD`, `JWT_SECRET`); o `.env` real nunca vai para o Git |
| `deploy/deploy.sh` | Constrói a imagem, envia pronta para a VPS (a VPS tem pouca memória para compilar), cria o `.env` com segredos aleatórios na primeira vez e sobe tudo |
| `.github/workflows/ci-cd.yml` | O pipeline |

## Configuração única no GitHub

Em **Settings → Secrets and variables → Actions**:

| Tipo | Nome | Valor |
|---|---|---|
| Secret | `VPS_HOST` | endereço da VPS |
| Secret | `VPS_USER` | usuário SSH (`ubuntu` ou `opc` na Oracle) |
| Secret | `VPS_SSH_KEY` | conteúdo da **chave privada** autorizada na VPS |
| Variable | `DEPLOY_ENABLED` | `true` para ligar o deploy |

## Exposição pública (Cloudflare Tunnel)

A API **não abre porta para a internet**. No `compose.prod.yaml` ela publica só em `127.0.0.1:8080`, e o `cloudflared` da própria VPS a expõe com HTTPS:

1. No painel Cloudflare Zero Trust → Tunnels → o túnel da VPS → **Public Hostname**.
2. Subdomínio/domínio de sua escolha, serviço `HTTP` e URL `localhost:8080`.
3. Use `https://seu-subdominio` como `API_BASE_URL` do app.

> Cuidado: o Docker ignora o `ufw` ao publicar portas. Por isso a porta é ligada em `127.0.0.1`; nunca use `8080:8080` sem o prefixo.

## Site público e download do app

A raiz `https://clinics.marciodev.com/` é uma página de download (arquivo `src/main/resources/static/index.html`):

| Rota | O que serve |
|---|---|
| `/` | Página de download: APK do Android, PWA no iPhone e link da documentação |
| `/app/` | App web (PWA), gerado pelo Flutter e embutido na imagem |
| `/docs` | Redireciona para a documentação OpenAPI (Swagger UI) |
| `/api/v1/...` | A API |

O app web **não é versionado** aqui: o job de deploy faz checkout do repositório do app, roda `deploy/build-web.sh` e a imagem já sai com `/app/`. Para um deploy manual, rode `deploy/build-web.sh` antes de `deploy/deploy.sh`. O APK vem da release `app-latest` do repositório do app (veja `docs/DISTRIBUICAO.md` lá).

## Deploy manual

```bash
deploy/deploy.sh usuario@IP_DA_VPS caminho/da/chave
```

## Operação

```bash
ssh usuario@IP_DA_VPS
cd /opt/clinics-backend
sudo docker compose -f compose.prod.yaml logs -f api    # logs
sudo docker compose -f compose.prod.yaml ps             # estado
sudo docker compose -f compose.prod.yaml restart api    # reiniciar
```

O banco e as fotos de perfil ficam em volumes Docker e sobrevivem a novos deploys.

## Segurança e próximos passos

- O HTTPS vem do Cloudflare Tunnel; nada de certificado na VPS.
- O Swagger e o `/v3/api-docs` estão públicos; considere desligá-los em produção (`springdoc.api-docs.enabled=false`).
- O banco não expõe porta para fora da VPS.
