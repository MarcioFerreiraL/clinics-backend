# 08 · Diagramas de Sequência

Os fluxos abaixo mostram como uma requisição atravessa as camadas (`controller` → `service` → `repository`), com as regras de negócio nas classes de `model`. Cada diagrama corresponde ao código real; os nomes dos participantes são as classes em `src/main/java/br/com/clinics`.

## 1. Cadastro e login (UC01, UC02)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as AuthController
    participant S as RegisterUserService
    participant D as Phone / Cpf / PasswordPolicy / User (model)
    participant R as UserRepository (JPA)
    participant H as BcryptPasswordHasher
    participant T as JwtTokenIssuer

    P->>C: POST /auth/register {full_name, phone, cpf, email, birth_date, password}
    C->>C: Bean Validation (campos obrigatórios)
    C->>S: register(RegisterUserCommand)
    S->>D: Phone.of(raw), Cpf.of(raw) e PasswordPolicy.validate()
    D-->>S: telefone normalizado (5581...) e CPF válido, ou ValidationException
    S->>R: existsByPhone(phone) e existsByCpf(cpf)
    alt telefone ou CPF já cadastrado
        S-->>C: ConflictException PHONE_ALREADY_REGISTERED / CPF_ALREADY_REGISTERED
        C-->>P: 409 problem+json
    else livres
        S->>H: hash(password) — BCrypt custo 10
        S->>D: User.register(...) com papel PATIENT
        S->>R: existsByEmailExcluding(email, id)
        Note over S,R: e-mail em uso gera EMAIL_ALREADY_IN_USE (409)
        S->>R: save(user)
        S->>T: issue(user) — sub, role, exp
        S-->>C: AuthResult
        C-->>P: 201 {token, user}
    end
```

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as AuthController
    participant S as AuthenticateService
    participant R as UserRepository
    participant H as BcryptPasswordHasher
    participant T as JwtTokenIssuer

    P->>C: POST /auth/login {phone, password}
    C->>S: authenticate(AuthenticateCommand)
    S->>R: findByPhone(Phone.of(phone))
    Note over S,H: Se o telefone não existe, compara com um hash descartável<br/>para igualar o tempo de resposta (RN06)
    S->>H: matches(password, hash)
    alt credenciais inválidas
        S-->>C: AuthenticationFailedException
        C-->>P: 401 INVALID_CREDENTIALS (mesma resposta para os dois casos)
    else válidas
        S->>T: issue(user)
        C-->>P: 200 {token, user}
    end
```

## 2. Acesso a uma rota protegida (JWT)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant F as Filtro OAuth2 Resource Server
    participant C as MeController
    participant S as ManageProfileService
    participant R as UserRepository

    P->>F: GET /me — Authorization: Bearer <token>
    F->>F: valida assinatura HS256 e expiração
    alt token ausente, inválido ou expirado
        F-->>P: 401 UNAUTHENTICATED (problem+json)
    else token válido
        F->>C: Jwt (sub = id do usuário, role)
        C->>S: getProfile(UUID.fromString(sub))
        Note over C,S: O id vem sempre do token,<br/>nunca da URL ou do corpo (RN35)
        S->>R: findById(id)
        C-->>P: 200 perfil
    end
```

## 3. Buscar e comparar exames (UC03)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as ExamController
    participant S as SearchExamsService
    participant R as ExamOfferRepository (SQL)
    participant D as ExamOffer / ExamSort / GeoPoint (model)

    P->>C: GET /exams/search?query&category&sort_by&lat&lng&neighborhood&min_price&max_price&min_rating&best_price_only
    C->>S: search(SearchExamsQuery)
    S->>D: ExamCategory.parse(), ExamSort.parse(), new GeoPoint(lat,lng)
    D-->>S: valida — sort=distance sem lat e lng gera LOCATION_REQUIRED (400)
    S->>R: findAvailable(texto, categoria)
    Note over R: Uma consulta: funções de janela calculam<br/>AVG e MIN por tipo de exame, unaccent na busca
    R-->>S: ofertas com average_price e min_price
    S->>S: filtra por bairro, faixa de preço, nota mínima e melhor preço
    loop para cada oferta
        S->>D: distanceKmFrom(origem) — Haversine
    end
    S->>D: ordena com ExamSort.comparator()
    S->>S: pagina em memória (size ≤ 50)
    S-->>C: PageResult
    C-->>P: 200 {total, data[{price, average_price, savings, best_price, distance_km}]}
```

## 4. Contato por WhatsApp e economia (UC07, UC08)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as ContactController
    participant S as RegisterContactService
    participant L as ContactableExamLookup
    participant K as GetExamDetailsService
    participant W as WhatsAppLinkBuilder (model)
    participant E as ContactEventRepository
    participant SC as SavingsController
    participant GS as GetSavingsService

    P->>C: POST /exams/{id}/contacts (token opcional)
    C->>C: userId = sub do JWT, ou null para visitante
    C->>S: register(examId, userId)
    S->>L: findById(examId)
    L->>K: getById(examId, null, null) — reutiliza o service do catálogo
    K-->>L: ExamDetails (oferta com preço, economia e WhatsApp)
    L-->>S: ContactableExam (só o que o contato usa)
    Note over L,S: EXAM_NOT_FOUND (404) se a oferta não existe
    S->>W: build(whatsapp, nome, preço)
    W-->>S: https://wa.me/55...?text=<mensagem codificada>
    S->>E: save(ContactEvent.of(...) com preço e economia congelados)
    S-->>C: ContactResult
    C-->>P: 201 {contact_id, whatsapp_url, estimated_savings}
    Note over P: O app abre o link com url_launcher
    P->>SC: GET /me/savings
    SC->>GS: getSavings(userId do JWT)
    GS->>E: summarizeByUser(userId)
    E-->>GS: SavingsSummary
    SC-->>P: 200 {total_savings, contacts_count}
```

## 5. Avaliar clínica (UC10)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as ReviewController
    participant S as ReviewClinicService
    participant D as Review / RatingStats (model)
    participant R as ReviewRepository (SQL)

    P->>C: PUT /clinics/{id}/reviews/me {rating, comment}
    C->>S: submit(userId, clinicId, rating, comment)
    Note over S,R: Tudo em uma única transação
    S->>R: clinicExists(id)
    S->>R: findByUserAndClinic(userId, clinicId)
    alt já avaliou
        S->>D: review.edit(rating, comment) — substitui
    else primeira avaliação
        S->>D: Review.create(...) — valida nota 1..5 e comentário ≤ 1000
    end
    S->>R: save(review) — INSERT ... ON CONFLICT (user, clínica) DO UPDATE
    S->>R: statsFor(clinicId) → soma e contagem
    S->>D: RatingStats.average() — 1 casa decimal, HALF_UP
    S->>R: updateClinicRating(clinicId, stats)
    S-->>C: SubmitResult(review, created)
    C-->>P: 201 se criou, 200 se substituiu
```

## 6. Atualizar foto de perfil (UC11)

```mermaid
sequenceDiagram
    autonumber
    actor P as Paciente (app)
    participant C as MeController
    participant S as ManageProfileService
    participant D as AvatarImage (model)
    participant A as LocalAvatarStorage
    participant R as UserRepository

    P->>C: PUT /me/avatar (multipart, parte "file")
    C->>S: changeAvatar(userId, bytes)
    S->>D: AvatarImage.of(bytes)
    Note over D: Detecta PNG/JPEG pelos bytes iniciais<br/>e rejeita arquivos > 2 MB (RN34)
    D-->>S: imagem válida ou INVALID_AVATAR (400)
    S->>R: findById(userId)
    S->>A: save(userId, image) → /files/avatars/<id>-<rand>.png
    S->>R: save(user com novo avatar_url)
    S->>A: delete(foto anterior)
    C-->>P: 200 perfil com avatar_url
```
