# Clinics Backend

API REST do Clinics, app que compara preços de exames e consultas em clínicas de Surubim-PE e leva o paciente ao WhatsApp da clínica. Java 25, Spring Boot 4, PostgreSQL, Flyway, JWT. O app Flutter fica em `../clinics-mobile`. Documentação completa em `docs/` (01 a 11: requisitos, regras de negócio, modelo de dados, arquitetura, API, deploy, testes).

## Comandos

```bash
docker compose up -d postgres      # PostgreSQL na porta 5433
set -a && . ./.env && set +a       # carrega o .env (copie de .env.example)
./mvnw spring-boot:run             # API em http://localhost:8080
./mvnw test                        # mais de 170 testes pela API (JUnit, MockMvc, Testcontainers)
```

Endpoints úteis: `/swagger-ui.html`, `/v3/api-docs`, `/actuator/health`.

## Arquitetura

- Um pacote por funcionalidade (`user`, `catalog`, `engagement`, `clinicadmin`, mais `common`), cada um com controller + service + entidades e repositórios. O service tem as regras e acessa o banco só por repositórios Spring Data JPA (escolha do Márcio); o que o JPQL não cobre usa `@Query` nativa. Entidades guardam FK como `UUID`, sem `@ManyToOne`. Mantenha assim: nada de criar DTOs ou interfaces de service sem necessidade.
- Não defina `hibernate.jdbc.time_zone`: desloca as colunas `TIME` dos horários das clínicas. Quando outra leitura na mesma transação depender da escrita, use `saveAndFlush`.
- Escrita que pode chegar em dobro (toque duplo no app) não usa "verifica e depois grava": favoritos e avaliações usam `ON CONFLICT` no repositório, e a nota da clínica é recalculada com a linha travada (`ConcurrentWritesIntegrationTest`).
- O contrato da API (rotas, JSON `snake_case`, códigos de erro) é usado pelo app publicado: não mude sem ajustar o `clinics-mobile`.
- O backend antigo (100 classes, em camadas) está na branch `legado` e na tag `legado-2026-10-27`.
- Migrations no Flyway: nunca editar uma migration já aplicada, criar uma nova.
- Autenticação JWT. Configuração por variáveis de ambiente (`PORT`, `AVATARS_DIR` etc., ver README).

## Deploy

- CI/CD no GitHub Actions (`.github/workflows/ci-cd.yml`; teste de carga em `performance.yml`, k6 em `loadtest/`).
- Produção: VPS Oracle, acessível por `ssh vps-oracle`, em `/opt/clinics-backend`, com `compose.prod.yaml`, atrás de Cloudflare Tunnel. Detalhes em `docs/09-deploy-e-pipeline.md`.
- O banco de produção roda no container `clinics-backend-postgres-1`. Nunca rodar `DROP`, `TRUNCATE` ou migration destrutiva sem confirmação.

## Cuidados

- `.env` é segredo e não vai para o git. Não imprimir nem repetir valores dele.
- Mudou comportamento de negócio? Atualize `docs/03-regras-de-negocio.md` e os testes correspondentes (`docs/10-testes-e-rastreabilidade.md`).
- Dumps do banco (`../clinics-backup.sql`) contêm dados reais: não commitar.
