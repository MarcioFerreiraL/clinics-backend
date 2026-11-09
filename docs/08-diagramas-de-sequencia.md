# 08 · Diagramas de Sequência

Os fluxos abaixo mostram como uma requisição passa pelo controller e pelo service do pacote correspondente até o PostgreSQL. O service chega ao banco sempre pelos repositórios Spring Data JPA, representados em um único participante. Os participantes são as classes em `src/main/java/br/com/clinics`.

## 1. Cadastro e login (UC01, UC02)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as UserController
    participant S as UserService
    participant DB as Repositórios / PostgreSQL

    P->>C: POST /auth/register {full_name, phone, cpf, email, birth_date, password}
    C->>C: Bean Validation (campos obrigatórios)
    C->>S: register(...)
    S->>S: parsePhone, parseCpf, validatePassword
    Note over S: telefone vira 5581..., CPF só dígitos;<br/>formato inválido gera 400 (INVALID_PHONE, INVALID_CPF, WEAK_PASSWORD)
    S->>DB: telefone ou CPF já existem?
    alt já cadastrado
        S-->>C: 409 PHONE_ALREADY_REGISTERED / CPF_ALREADY_REGISTERED
        C-->>P: 409 problem+json
    else livres
        S->>S: valida nome, e-mail e nascimento
        S->>DB: e-mail em uso? (409 EMAIL_ALREADY_IN_USE)
        S->>DB: INSERT users (senha com BCrypt custo 10, papel PATIENT)
        C->>S: issueToken(user) — sub, role, exp
        C-->>P: 201 {token, user}
    end
```

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as UserController
    participant S as UserService
    participant DB as Repositórios / PostgreSQL

    P->>C: POST /auth/login {phone, password}
    C->>S: login(phone, password)
    S->>DB: SELECT usuário pelo telefone
    Note over S: Se o telefone não existe, compara com um hash descartável<br/>para igualar o tempo de resposta (RN06)
    alt credenciais inválidas
        S-->>C: 401 INVALID_CREDENTIALS (mesma resposta para os dois casos)
        C-->>P: 401 problem+json
    else válidas
        C->>S: issueToken(user)
        C-->>P: 200 {token, user}
    end
```

## 2. Acesso a uma rota protegida (JWT)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant F as Filtro OAuth2 Resource Server
    participant C as UserController
    participant S as UserService

    P->>F: GET /me — Authorization: Bearer <token>
    F->>F: valida assinatura HS256 e expiração (SecurityConfig)
    alt token ausente, inválido ou expirado
        F-->>P: 401 UNAUTHENTICATED (problem+json)
    else token válido
        F->>C: Jwt (sub = id do usuário, role)
        C->>S: find(userId(jwt))
        Note over C,S: O id vem sempre do token,<br/>nunca da URL ou do corpo (RN35)
        C-->>P: 200 perfil
    end
```

## 3. Buscar e comparar exames (UC03)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as CatalogController
    participant S as CatalogService
    participant DB as Repositórios / PostgreSQL

    P->>C: GET /exams/search?query&category&sort_by&lat&lng&neighborhood&min_price&max_price&min_rating&best_price_only
    C->>S: search(...)
    S->>S: valida categoria, ordenação, lat/lng e faixas
    Note over S: sort_by=distance sem lat e lng gera LOCATION_REQUIRED (400)
    S->>DB: SQL OFFERS + filtro de texto e apelidos (unaccent)
    Note over DB: funções de janela calculam AVG e MIN por tipo de exame
    DB-->>S: ofertas com average_price e min_price
    S->>S: filtra por bairro, preço, nota e melhor preço
    S->>S: calcula distância (Haversine), ordena e pagina (size ≤ 50)
    C-->>P: 200 {total, data[{price, average_price, savings, best_price, distance_km}]}
```

## 4. Contato por WhatsApp e economia (UC07, UC08)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as CatalogController
    participant S as CatalogService
    participant EC as EngagementController
    participant ES as EngagementService
    participant DB as Repositórios / PostgreSQL

    P->>C: POST /exams/{id}/contacts (token opcional)
    C->>S: contact(examId, userId ou null)
    S->>DB: busca a oferta (EXAM_NOT_FOUND se não existe)
    S->>S: monta https://wa.me/55...?text=<mensagem codificada>
    S->>DB: INSERT contact_events com preço e economia congelados
    C-->>P: 201 {contact_id, whatsapp_url, estimated_savings}
    Note over P: O app abre o link com url_launcher
    P->>EC: GET /me/savings
    EC->>ES: savings(userId do JWT)
    ES->>DB: SUM(estimated_savings), COUNT(*)
    EC-->>P: 200 {total_savings, contacts_count}
```

## 5. Avaliar clínica (UC10)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as EngagementController
    participant S as EngagementService
    participant DB as Repositórios / PostgreSQL

    P->>C: PUT /clinics/{id}/reviews/me {rating, comment}
    C->>S: submitReview(userId, clinicId, rating, comment)
    Note over S,DB: Tudo em uma única transação
    S->>DB: a clínica existe? (CLINIC_NOT_FOUND)
    S->>S: nota 1..5 e comentário ≤ 1000
    S->>DB: já existe avaliação deste paciente?
    S->>DB: grava a avaliação (nova ou editada; UNIQUE por usuário e clínica)
    S->>DB: atualiza clinics: rating = média com 1 casa, review_count
    C-->>P: 201 se criou, 200 se substituiu
```

## 6. Atualizar foto de perfil (UC11)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as UserController
    participant S as UserService
    participant FS as Disco (AVATARS_DIR)
    participant DB as Repositórios / PostgreSQL

    P->>C: PUT /me/avatar (multipart, parte "file")
    C->>S: changeAvatar(userId, bytes)
    S->>S: detecta PNG/JPEG pelos bytes e rejeita > 2 MB (RN34)
    Note over S: inválido gera INVALID_AVATAR (400)
    S->>FS: grava <id>-<rand>.png
    S->>DB: UPDATE users SET avatar_url = /files/avatars/...
    S->>FS: apaga a foto anterior
    C-->>P: 200 perfil com avatar_url
```
