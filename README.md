# Clinics Backend

API REST do **Clinics**, aplicativo que compara preços de exames e consultas em clínicas de Surubim-PE e leva o paciente direto ao WhatsApp da clínica.

## Integrantes

- Márcio Ferreira Lima
- Lucas Nascimento Barros

## Tecnologias

Java 25 · Spring Boot 4.1 · Spring Security (JWT) · Spring Data JPA · PostgreSQL 17 · Flyway · springdoc OpenAPI · JUnit 5 · Testcontainers · ArchUnit · Docker Compose

## Como executar

Pré-requisitos: JDK 25 e Docker.

```bash
docker compose up -d        # PostgreSQL na porta 5433
./mvnw spring-boot:run      # API em http://localhost:8080
./mvnw test                 # testes (usam Testcontainers)
```

Variáveis de ambiente opcionais: `DB_URL`, `DB_USER`, `DB_PASSWORD`, `JWT_SECRET` (mínimo 32 bytes), `PORT`.

## Documentação

Em [`docs/`](docs): requisitos, histórias de usuário, regras de negócio, casos de uso, modelo de dados (MER) e arquitetura com diagrama de classes.

## Arquitetura

Hexagonal (portas e adaptadores), organizada por funcionalidade: `identity`, `catalog` e `engagement`. Detalhes em [`docs/06-arquitetura-e-classes.md`](docs/06-arquitetura-e-classes.md).

## Status

Entrega 2 em andamento. Casos de uso implementados: UC01 Cadastrar conta e UC02 Autenticar.
