# Plano: RBAC do Clinics no modelo do SIEP

Status: **aprovado e implementado** na branch `feat/rbac`. Mudança em relação ao plano: o perfil ficou em `GET /me/access` (`/auth/**` é público no `SecurityConfig`), e entrou `GET /clinic/{clinicId}/dashboard` para exercitar o escopo.

## Objetivo

Levar o controle de acesso do Clinics ao modelo do estudo de caso "RBAC no SIEP": duas camadas (papel e permissão com escopo), permissões em tabelas e administração pela API. Manter simples: sem bibliotecas novas, sem DTOs ou interfaces extras, seguindo o estilo atual (um pacote por funcionalidade, service com repositórios Spring Data).

## Como é hoje

- `users.role`: `PATIENT`, `CLINIC_MANAGER`, `ADMIN`; vai no JWT e vira `ROLE_*`.
- Única checagem: `/api/v1/clinic/me/**` exige `CLINIC_MANAGER` (SecurityFilterChain).
- Gestor ligado à clínica por `clinics.owner_user_id` (índice único, V10).
- `ADMIN` existe, mas nenhuma rota o usa. Não há permissões, escopo nem auditoria.

## Como fica

### Camada 1: papel (SecurityFilterChain, sem mudar rotas existentes)
- `/clinic/me/**` passa a aceitar `CLINIC_MANAGER` e `ADMIN`.
- `/api/v1/admin/**` exige `ADMIN`.
- Contrato JSON e caminhos atuais não mudam (o app publicado depende deles).

### Camada 2: permissão e escopo (dentro do service)
Novo pacote `access` com um `AccessService`:
- `hasPermission(userId, "exame.editar")`: usuário → cargo → permissões, lido do banco a cada requisição.
- `canAccessClinic(userId, clinicId)`: `all` para Administrador; `own_clinic` para os demais, pela lotação.
- Sem permissão: **403** `ACCESS_DENIED`. Clínica fora do escopo: **404** `CLINIC_NOT_FOUND`.

### Modelo de dados (migration nova `V11__rbac.sql`, só adiciona)
| Tabela | Colunas |
|---|---|
| `positions` | id, name (único), visibility_scope (`all` ou `own_clinic`) |
| `permissions` | id, resource, action, label; único (resource, action) |
| `position_permissions` | position_id, permission_id |
| `internal_users` | user_id (PK), position_id, clinic_id (nulo para Administrador) |
| `permission_audit_logs` | id, actor_user_id, position_id, permission_id, action (`grant`/`revoke`/`position_change`), created_at |

Seed: cargos **Administrador**, **Gestor da clínica**, **Atendente**; as 9 permissões; matriz abaixo. A migration copia cada `clinics.owner_user_id` para `internal_users` como Gestor, e cada `users.role = 'ADMIN'` como Administrador. `owner_user_id` fica na tabela, sem uso, para remoção em migration futura.

### Matriz inicial
| Permissão | Administrador | Gestor | Atendente |
|---|:-:|:-:|:-:|
| clinica.ver | x | x | x |
| clinica.editar | x | x | |
| exame.ver | x | x | x |
| exame.editar | x | x | |
| exame.desativar | x | x | |
| indicadores.ver | x | x | x |
| conta.listar | x | | |
| conta.gerenciar | x | | |
| permissao.gerenciar | x | | |

Escopo: Administrador `all`; Gestor e Atendente `own_clinic`.

### Rotas novas (aditivas)
- `GET /api/v1/auth/profile`: usuário, cargo, clínica e lista de permissões (para o app esconder botões).
- `GET /api/v1/admin/accounts` (`conta.listar`); `PUT /api/v1/admin/accounts/{id}/position` (`conta.gerenciar`).
- `GET /api/v1/admin/positions` e `PUT /api/v1/admin/positions/{id}/permissions/{permissionId}` e `DELETE` (`permissao.gerenciar`); cada mudança grava em `permission_audit_logs`.

### Efeito no código existente
- `ClinicAdminService`: `clinic(userId)` passa a usar a lotação (`internal_users`) em vez de `owner_user_id`; cada operação chama `hasPermission`. Atendente lê, não edita.
- `ClinicRepository.findByOwnerUserId` deixa de ser usado.
- `CLAUDE.md` e `docs/` (03, 05, 07, 10) atualizados.

## Testes
- Novo `RbacIntegrationTest` (Testcontainers/MockMvc, como os atuais): 401 sem token, 403 por cargo, 404 fora do escopo, Administrador vê todas as clínicas, mudança na matriz vale na requisição seguinte, auditoria gravada, migration preserva gestores existentes.
- Ajustar `ClinicManagementIntegrationTest` e `DataModelIntegrationTest`, que usam `owner_user_id`.
- Meta: todos os testes atuais continuam passando; `./mvnw test` verde.

## Git e entrega
1. Branch `feat/rbac` a partir da `develop`; commits curtos em português, **sem coautoria**.
2. Testes verdes; merge de `feat/rbac` na `develop` localmente.
3. O `push` é seu, no terminal (o hook global bloqueia push por mim). Nada vai à `main` nem à VPS sem você.
4. Por último: `docs/RBAC no Clinics.md` (estudo de caso, mesma estrutura do SIEP, autores Márcio e Lucas), com o código real.
5. Notion: atualizar os cards do Kanban (matriz, testes, apresentação) e criar o relatório como subpágina do projeto.
6. `CLAUDE.md` do backend atualizado com o que for aprendido (modelo RBAC, regra 403/404, sem coautoria nos commits).

## Riscos
- Migration em produção: só cria tabelas e insere linhas; mesmo assim roda no deploy da `main`, então você aprova o merge.
- Gestor sem linha em `internal_users` perderia acesso: a migration cobre isso e há teste.
- Mais uma consulta ao banco por requisição protegida: aceitável para a escala do projeto.
