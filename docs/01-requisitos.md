# 01 · Documento de Requisitos

**Projeto:** Clinics, comparador de preços de exames e consultas em Surubim-PE
**Escopo desta versão:** visão do **paciente** (Entrega 2). A visão da clínica (gestão de exames) está fora do escopo e é tratada em `99-fora-de-escopo.md` no futuro.

## 1. Visão geral

Pacientes de cidades pequenas não sabem quanto cada clínica cobra por um exame e perdem tempo ligando de clínica em clínica. O Clinics reúne os preços, mostra a comparação lado a lado (com distância e avaliação) e leva o paciente direto ao WhatsApp da clínica escolhida.

## 2. Atores

| Ator | Descrição |
|---|---|
| **Visitante** | Pessoa sem login. Pode buscar, comparar e ver detalhes. |
| **Paciente** | Usuário autenticado (`PATIENT`). Pode favoritar, avaliar, editar perfil e registrar contatos. |
| **Sistema** | Executa rotinas automáticas (recalcular nota da clínica, registrar histórico de preço). |
| *Gestor de clínica* | *Fora do escopo desta entrega.* |

## 3. Requisitos funcionais

Prioridade pelo método MoSCoW: **M** (deve), **S** (deveria), **C** (poderia). A coluna *Entrega* indica quando entra.

| ID | Requisito | Prior. | Entrega |
|---|---|---|---|
| RF01 | O sistema deve permitir o cadastro de paciente com nome, telefone e senha. | M | 2 |
| RF02 | O sistema deve autenticar o paciente por telefone e senha e emitir um token JWT. | M | 2 |
| RF03 | O sistema deve listar exames com busca por texto, filtro por categoria e ordenação por preço, avaliação, nome ou distância. | M | 2 |
| RF04 | A busca deve devolver o preço médio local do exame e a economia de cada oferta em relação à média. | S | 2 |
| RF05 | O sistema deve exibir os detalhes de uma oferta de exame: preparo, formas de pagamento, clínica e se está aberta agora. | M | 2 |
| RF06 | O sistema deve exibir o perfil público da clínica: dados, horários, nota e quantidade de avaliações. | M | 2 |
| RF07 | O sistema deve listar os exames em alta na semana. | S | 2 |
| RF08 | O paciente deve poder favoritar e desfavoritar um exame e listar seus favoritos. | M | 2 |
| RF09 | O paciente deve poder avaliar uma clínica (nota 1 a 5 e comentário) e ver as avaliações. | S | 2 |
| RF10 | O sistema deve registrar o contato via WhatsApp e devolver o link `wa.me` com mensagem pronta. | M | 2 |
| RF11 | O sistema deve calcular o total economizado pelo paciente com base nos contatos. | C | 2 |
| RF12 | O paciente deve poder consultar e editar seus dados, alterar a senha e atualizar a foto. | M | 2 |
| RF13 | O sistema deve guardar o histórico de preços de cada oferta. | S | 2 |
| RF14 | O app deve mostrar a distância até a clínica usando o GPS do aparelho. | M | 2 |
| RF15 | O app deve permitir busca por voz. | S | 2 |
| RF16 | O app deve permitir tirar ou escolher a foto de perfil. | S | 2 |
| RF17 | O app deve notificar localmente a queda de preço de favoritos e lembrar o jejum do exame. | C | 2 |
| RF18 | Recuperação de senha por código. | S | 3 |
| RF19 | Histórico de buscas do paciente. | C | 3 |
| RF20 | Registro de consentimento LGPD, exportação e exclusão de dados. | S | 3 |
| RF21 | Controle de acesso por papéis (RBAC). | C | 3 |
| RF22 | Refresh token e revogação de sessão. | S | 3 |

## 4. Requisitos não funcionais

| ID | Requisito | Como será atendido |
|---|---|---|
| RNF01 | **Segurança de credenciais:** senhas com BCrypt (custo ≥ 10); senha nunca é retornada nem registrada em log. | `BCryptPasswordEncoder` |
| RNF02 | **Autenticação stateless** com JWT assinado (HS256, expiração curta). | Spring Security OAuth2 Resource Server |
| RNF03 | **Desempenho:** busca com resposta p95 < 300 ms para até 5 mil ofertas. | Índices, paginação obrigatória |
| RNF04 | **Paginação** em toda listagem (`page`, `size`, máximo 50). | `Pageable` |
| RNF05 | **Integridade:** o esquema é versionado e o Hibernate só valida (`ddl-auto=validate`). | Flyway |
| RNF06 | **Contrato documentado:** OpenAPI publicado em `/swagger-ui`. | springdoc |
| RNF07 | **Erros padronizados** conforme RFC 9457 (`application/problem+json`). | `ProblemDetail` |
| RNF08 | **Testabilidade:** testes unitários de domínio e testes de integração com PostgreSQL real. | JUnit 5 + Testcontainers |
| RNF09 | **LGPD:** coletar só os dados necessários; localização do paciente não é armazenada no servidor. | Latitude e longitude só como parâmetro de consulta |
| RNF10 | **Portabilidade:** `docker compose up` sobe o banco; a API se configura por variáveis de ambiente (12-factor). | `compose.yaml`, `application.yaml` |
| RNF11 | **Rastreabilidade:** código em inglês, mensagens ao usuário em pt-BR, commits em Conventional Commits (pt-BR). | Convenção do repositório |

## 5. Restrições

- PostgreSQL como banco de dados.
- Back-end em Spring Boot; app em Flutter, em repositório separado.
- A busca é restrita a Surubim-PE nesta versão (o aviso "procurar também em Limoeiro ou Casinhas" é só visual).

## 6. Premissas

- Os preços são informados pelas clínicas. Nesta entrega os dados vêm de um seed de demonstração.
- A clínica atende o agendamento por WhatsApp. O app **não** agenda nem processa pagamento.
