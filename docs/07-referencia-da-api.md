# 07 · Referência da API

Referência gerada a partir da aplicação em execução (as respostas abaixo são reais, abreviadas onde indicado por `...`).
A versão interativa fica em `/swagger-ui.html` e o contrato em `/v3/api-docs`.

**Base URL local:** `http://localhost:8080/api/v1`

## 1. Convenções

| Tema | Regra |
|---|---|
| Formato | JSON em `snake_case`. Campos nulos são omitidos da resposta. |
| Autenticação | Cabeçalho `Authorization: Bearer <token>`. O token vem de `register` ou `login` e vale 1 hora. |
| Identidade | Em `/me/**` o usuário é sempre o dono do token. Nenhum endpoint recebe o id do usuário na URL ou no corpo (RN35). |
| Paginação | `page` (começa em 0) e `size` (padrão 20, máximo 50). A resposta traz `total`, `page`, `size`, `total_pages` e `data`. |
| Dinheiro | Número decimal em reais com 2 casas (`90.0`). |
| Datas | ISO 8601 em UTC (`2026-10-13T01:57:45Z`). Horários de funcionamento em `HH:mm` no fuso `America/Recife`. |
| Erros | RFC 9457 (`application/problem+json`) com o campo `code` para o app tratar. Ver seção 4. |

**Legenda de acesso:** 🌐 público · 🔓 token opcional · 🔒 exige token.

## 2. Resumo dos endpoints

| UC | Método e caminho | Acesso | Sucesso | Descrição |
|---|---|---|---|---|
| UC01 | `POST /auth/register` | 🌐 | 201 | Cadastra paciente e já devolve o token |
| UC02 | `POST /auth/login` | 🌐 | 200 | Autentica com telefone e senha |
| UC03 | `GET /exams/search` | 🌐 | 200 | Busca e compara ofertas |
| UC13 | `GET /neighborhoods` | 🌐 | 200 | Bairros com clínicas |
| UC04 | `GET /exams/{id}` | 🌐 | 200 | Detalhes da oferta e da clínica |
| UC05 | `GET /clinics/{id}` | 🌐 | 200 | Perfil público, horários e exames |
| UC06 | `GET /exam-types/trending` | 🌐 | 200 | Exames em alta na semana |
| UC07 | `POST /exams/{id}/contacts` | 🔓 | 201 | Registra contato e devolve o link do WhatsApp |
| UC08 | `GET /me/savings` | 🔒 | 200 | Total economizado |
| UC09 | `GET /me/favorites` | 🔒 | 200 | Lista favoritos |
| UC09 | `PUT /me/favorites/{examTypeId}` | 🔒 | 204 | Favorita (idempotente) |
| UC09 | `DELETE /me/favorites/{examTypeId}` | 🔒 | 204 | Remove (idempotente) |
| UC10 | `GET /clinics/{id}/reviews` | 🌐 | 200 | Lista avaliações |
| UC10 | `PUT /clinics/{id}/reviews/me` | 🔒 | 201 / 200 | Cria ou substitui a minha avaliação |
| UC10 | `GET /clinics/{id}/reviews/me` | 🔒 | 200 | Minha avaliação |
| UC11 | `GET /me` | 🔒 | 200 | Meu perfil |
| UC11 | `PUT /me` | 🔒 | 200 | Edita nome, e-mail e nascimento |
| UC11 | `PUT /me/avatar` | 🔒 | 200 | Foto de perfil (multipart) |
| UC12 | `PUT /me/password` | 🔒 | 204 | Troca a senha |

Arquivos de avatar são servidos em `GET /files/avatars/{arquivo}` (público). O campo `avatar_url` guarda o caminho relativo.

## 3. Detalhamento

### UC01 · `POST /auth/register`
```json
// requisição
{ "full_name": "Maria Silva", "phone": "(81) 99876-5432", "cpf": "529.982.247-25",
  "email": "maria@email.com", "birth_date": "1990-05-20", "password": "Senha1234" }
```
```json
// 201
{
  "token": "eyJhbGciOiJIUzI1NiJ9...",
  "token_type": "Bearer",
  "expires_at": "2026-10-13T02:57:34Z",
  "user": { "id": "12cbcc59-9b71-43ae-bc93-fd0c932cad45", "full_name": "Maria Silva",
            "phone": "(81) 99876-5432", "role": "PATIENT" }
}
```
Telefone e CPF aceitam qualquer formatação: o telefone é normalizado para dígitos com DDI 55 (RN01) e o CPF para 11 dígitos, com validação dos dígitos verificadores (RN37). E-mail e data de nascimento são obrigatórios. Senha: mínimo de 8 caracteres com letras e números (RN03). Erros: `INVALID_PHONE`, `INVALID_CPF`, `INVALID_EMAIL`, `INVALID_BIRTH_DATE`, `WEAK_PASSWORD`, `PHONE_ALREADY_REGISTERED`, `CPF_ALREADY_REGISTERED`, `EMAIL_ALREADY_IN_USE` (409), `VALIDATION_ERROR`.

### UC02 · `POST /auth/login`
Corpo `{ "phone": "...", "password": "..." }`. A resposta é igual à do cadastro. Falha devolve sempre `INVALID_CREDENTIALS` (401), sem revelar se o telefone existe (RN06).

O token tem as claims `sub` (id do usuário), `role`, `iat` e `exp`.

### UC03 · `GET /exams/search`

| Parâmetro | Tipo | Observação |
|---|---|---|
| `query` | texto | Nome clínico ou popular ("colesterol", "esteira", "chapa do braço"); ignora caixa e acentos; `%` e `_` são texto literal |
| `category` | texto | `Sangue`, `Raio-X`, `Consultas`, `Cardio` ou `Ultrassom` |
| `sort_by` | texto | `price` (padrão), `rating`, `name` ou `distance` |
| `lat`, `lng` | decimal | Obrigatórios com `sort_by=distance`; sempre juntos. Não são armazenados (RN20) |
| `neighborhood` | texto | Bairro da clínica; ignora caixa e acentos |
| `min_price`, `max_price` | decimal | Faixa de preço em reais |
| `min_rating` | decimal | Nota mínima da clínica (0 a 5) |
| `best_price_only` | booleano | Só as ofertas de menor preço de cada exame |
| `page`, `size` | inteiro | Paginação |

```json
// GET /exams/search?query=ultrassom abdominal&sort_by=distance&lat=-7.8335&lng=-35.7530&size=2
{
  "total": 5, "page": 0, "size": 2, "total_pages": 3,
  "data": [
    {
      "id": "cf5956f8-27c6-48d5-b66e-14f06d50fc85",
      "exam_type": { "id": "e2efd2c4-...", "name": "Ultrassom Abdominal Total", "category": "Ultrassom" },
      "clinic": { "id": "8dc21c45-...", "name": "Clínica Santa Mariazinha", "neighborhood": "Centro",
                  "rating": 4.8, "is_verified": true, "whatsapp_number": "5581999999999", "logo_url": "https://..." },
      "price": 90.0,
      "average_price": 118.0,
      "savings": 28.0,
      "best_price": true,
      "distance_km": 0.0,
      "preparation": "Jejum 8h • Laudo em 24h",
      "payment_methods": "Pix à vista ou até 3x no cartão"
    },
    ...
  ]
}
```
- `average_price` é a média de todas as ofertas disponíveis do mesmo exame (RN14); `savings = max(0, média − preço)` (RN15).
- `best_price` marca a oferta de menor preço.
- `distance_km` só aparece quando `lat` e `lng` são informados e a clínica tem coordenadas.

Erros: `INVALID_CATEGORY`, `INVALID_SORT`, `INVALID_PRICE_RANGE`, `INVALID_MIN_RATING`, `LOCATION_REQUIRED`, `LOCATION_INCOMPLETE`, `INVALID_COORDINATES`.

### UC13 · `GET /neighborhoods`
```json
[ { "name": "Centro", "clinics_count": 2 }, { "name": "Cohab", "clinics_count": 1 } ]
```
Alimenta o seletor de bairro do app. Ordem alfabética, sem acento.

### UC04 · `GET /exams/{id}` (aceita `lat` e `lng` opcionais)
```json
{
  "id": "cf5956f8-...",
  "exam_type": { "id": "e2efd2c4-...", "name": "Ultrassom Abdominal Total", "category": "Ultrassom",
                 "description": "Ultrassom com Doppler para avaliação de fígado, vesícula, rins..." },
  "price": 90.0, "average_price": 118.0, "savings": 28.0, "best_price": true,
  "preparation": "Jejum 8h • Laudo em 24h",
  "payment_methods": "Pix à vista ou até 3x no cartão",
  "clinic": {
    "id": "8dc21c45-...", "name": "Clínica Santa Mariazinha", "address": "Rua João Alfredo, 142",
    "neighborhood": "Centro", "phone_number": "(81) 3634-1122", "whatsapp_number": "5581999999999",
    "rating": 4.8, "review_count": 5, "is_verified": true,
    "latitude": -7.8333, "longitude": -35.7528,
    "open_now": false,
    "operating_hours": [ { "day_of_week": 1, "opens_at": "06:30", "closes_at": "18:00" }, ... ]
  }
}
```
`day_of_week` segue a ISO: 1 = segunda, 7 = domingo. `open_now` é calculado no servidor (RN22). Oferta inexistente ou indisponível: `EXAM_NOT_FOUND` (404).

### UC05 · `GET /clinics/{id}`
Devolve os dados da clínica, `open_now`, `operating_hours`, `exams_count` e a lista `exams` (cada item com `id`, `exam_type`, `price`, `best_price`, `preparation`, `payment_methods`), ordenada pelo nome do exame. Erro: `CLINIC_NOT_FOUND`.

### UC06 · `GET /exam-types/trending`
```json
[
  { "id": "e2efd2c4-...", "name": "Ultrassom Abdominal Total", "slug": "ultrassom-abdominal-total",
    "category": "Ultrassom", "min_price": 90.0, "offers_count": 5, "recent_contacts": 25 },
  ...
]
```
Até 5 itens, ordenados pelos contatos dos últimos 7 dias; empates desempatam pelo menor preço médio (RN30). `min_price` é o "a partir de".

### UC07 · `POST /exams/{id}/contacts`
Sem corpo. O token é opcional: com token, o contato fica ligado ao paciente e conta para a economia. Um token inválido continua resultando em 401.
```json
// 201
{
  "contact_id": "5edae4b1-3426-482e-9a16-ae4432a87ddb",
  "whatsapp_url": "https://wa.me/5581999999999?text=Ol%C3%A1%21%20Vi%20o%20exame%20Ultrassom%20Abdominal%20Total%20por%20R%24%2090%2C00%20no%20Clinics%20e%20gostaria%20de%20agendar.",
  "estimated_savings": 28.0
}
```

### UC08 · `GET /me/savings`
```json
{ "total_savings": 28.00, "contacts_count": 1 }
```
A economia de cada contato fica congelada no momento do contato (RN31).

### UC09 · Favoritos
`PUT` e `DELETE /me/favorites/{examTypeId}` respondem 204 mesmo se repetidos. O id é o do **tipo de exame** (`exam_type.id`), não o da oferta (RN23). Tipo inexistente: `EXAM_TYPE_NOT_FOUND`.
```json
// GET /me/favorites
{
  "total": 1, "page": 0, "size": 20, "total_pages": 1,
  "data": [ { "exam_type_id": "e2efd2c4-...", "name": "Ultrassom Abdominal Total",
              "slug": "ultrassom-abdominal-total", "category": "Ultrassom",
              "min_price": 90.0, "offers_count": 5, "favorited_at": "2026-10-13T01:57:45Z" } ]
}
```
Do mais recente para o mais antigo. O app compara `min_price` com o valor guardado localmente para disparar o alerta de queda de preço.

### UC10 · Avaliações
```json
// PUT /clinics/{id}/reviews/me  (201 na primeira vez, 200 ao reavaliar)
{ "rating": 5, "comment": "Atendimento rápido." }
```
```json
{ "id": "6d2002be-...", "clinic_id": "8dc21c45-...", "rating": 5, "comment": "Atendimento rápido.",
  "created_at": "2026-10-13T01:57:45Z", "updated_at": "2026-10-13T01:57:45Z" }
```
A nota (1 a 5) é obrigatória; o comentário tem até 1000 caracteres. Ao salvar, a nota média e a contagem da clínica são recalculadas na mesma transação (RN27).

`GET /clinics/{id}/reviews` é público e mostra o autor abreviado, por privacidade:
```json
{ "total": 6, "page": 0, "size": 2, "total_pages": 3,
  "data": [ { "id": "6d2002be-...", "rating": 5, "comment": "Atendimento rápido.",
              "author_name": "Maria S.", "created_at": "2026-10-13T01:57:45Z" }, ... ] }
```
`GET /clinics/{id}/reviews/me` devolve a minha avaliação ou `REVIEW_NOT_FOUND` (404), o que permite ao app pré-preencher a tela.

### UC11 · Perfil
```json
// GET /me
{ "id": "12cbcc59-...", "full_name": "Maria Silva", "phone": "(81) 99876-5432", "cpf": "529.982.247-25",
  "role": "PATIENT", "created_at": "2026-10-13T01:57:34Z" }
```
`email`, `birth_date` e `avatar_url` aparecem quando preenchidos. O CPF não é editável.

`PUT /me` aceita `full_name` (obrigatório), `email` e `birth_date` (`YYYY-MM-DD`). O telefone enviado é ignorado (RN32). E-mail vazio remove o e-mail.

`PUT /me/avatar` recebe `multipart/form-data` com a parte `file` (JPEG ou PNG, até 2 MB). O tipo é detectado pelo conteúdo do arquivo. A foto anterior é removida. Erro: `INVALID_AVATAR`.

### UC12 · `PUT /me/password`
```json
{ "current_password": "Senha1234", "new_password": "NovaSenha99" }
```
Resposta 204. Erros: `INVALID_CURRENT_PASSWORD`, `WEAK_PASSWORD`, `SAME_PASSWORD`.

## 4. Formato de erro e catálogo de códigos

```json
{
  "type": "https://clinics.app/errors/validation-error",
  "title": "Bad Request",
  "status": 400,
  "detail": "Há campos inválidos na requisição.",
  "instance": "/api/v1/auth/register",
  "code": "VALIDATION_ERROR",
  "errors": [ { "field": "full_name", "message": "Informe seu nome completo." } ]
}
```
`errors` só existe em `VALIDATION_ERROR`, e `field` usa os mesmos nomes `snake_case` do JSON enviado.

| Status | Códigos |
|---|---|
| 400 | `VALIDATION_ERROR`, `MALFORMED_REQUEST`, `INVALID_PHONE`, `INVALID_CPF`, `WEAK_PASSWORD`, `INVALID_NAME`, `INVALID_EMAIL`, `INVALID_BIRTH_DATE`, `INVALID_AVATAR`, `INVALID_CURRENT_PASSWORD`, `SAME_PASSWORD`, `INVALID_CATEGORY`, `INVALID_SORT`, `INVALID_PRICE_RANGE`, `INVALID_MIN_RATING`, `LOCATION_REQUIRED`, `LOCATION_INCOMPLETE`, `INVALID_COORDINATES`, `INVALID_RATING`, `COMMENT_TOO_LONG`, `CLINIC_WITHOUT_WHATSAPP` |
| 401 | `INVALID_CREDENTIALS` (login), `UNAUTHENTICATED` (token ausente, inválido ou expirado) |
| 403 | `ACCESS_DENIED` |
| 404 | `EXAM_NOT_FOUND`, `EXAM_TYPE_NOT_FOUND`, `CLINIC_NOT_FOUND`, `REVIEW_NOT_FOUND`, `USER_NOT_FOUND` |
| 409 | `PHONE_ALREADY_REGISTERED`, `CPF_ALREADY_REGISTERED`, `EMAIL_ALREADY_IN_USE` |
| 500 | `INTERNAL_ERROR` (sem detalhes técnicos na resposta) |

## 5. Como testar manualmente

```bash
docker compose up -d && ./mvnw spring-boot:run

# cadastro e guarda o token
TOKEN=$(curl -s -X POST localhost:8080/api/v1/auth/register -H 'Content-Type: application/json' \
  -d '{"full_name":"Maria Silva","phone":"(81) 99876-5432","cpf":"529.982.247-25","email":"maria@email.com","birth_date":"1990-05-20","password":"Senha1234"}' | jq -r .token)

# busca pública e rota protegida
curl "localhost:8080/api/v1/exams/search?query=hemograma"
curl localhost:8080/api/v1/me -H "Authorization: Bearer $TOKEN"
```
