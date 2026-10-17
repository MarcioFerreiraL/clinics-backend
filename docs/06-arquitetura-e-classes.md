# 06 · Arquitetura e Diagrama de Classes

## 1. Estilo arquitetural

**Arquitetura em camadas** (*layered*): cada requisição desce por `controller` → `service` → `repository`, e nunca sobe. As classes de `model` (entidades, objetos de valor e regras de negócio) e de `dto` (comandos e resultados) são compartilhadas pelas camadas.

```mermaid
flowchart TB
    CLI["App / cliente HTTP"]
    subgraph CTRL["controller"]
        REST["Controllers REST<br/>(@RestController, validação, DTOs de resposta)"]
    end
    subgraph SVC["service"]
        S["Services<br/>(@Service, @Transactional, orquestram as regras)"]
    end
    subgraph REPO["repository"]
        R["Repositórios<br/>(JdbcClient com SQL; Spring Data JPA para users)"]
    end
    DB[("PostgreSQL")]
    subgraph SHARED["Compartilhados"]
        M["model<br/>(entidades, records, regras)"]
        D["dto<br/>(comandos, resultados, requests/responses)"]
    end
    subgraph APOIO["Apoio técnico"]
        SEC["security<br/>(JwtTokenIssuer, BcryptPasswordHasher)"]
        STO["storage<br/>(LocalAvatarStorage)"]
        EXC["exception<br/>(DomainException, GlobalExceptionHandler)"]
    end
    CLI --> REST
    REST --> S
    S --> R
    R --> DB
    S --> SEC
    S --> STO
    REST -.-> D
    S -.-> M
    S -.-> D
    R -.-> M
```

- **controller**: recebe HTTP, aplica Bean Validation, extrai o id do usuário do JWT e converte o resultado do service em DTO de resposta.
- **service**: uma classe por funcionalidade (`RegisterUserService`, `SearchExamsService`, ...). Define a transação e chama as regras de `model`.
- **repository**: acesso ao banco. Só é chamado por services.
- **model**: regras de negócio puras (ex.: `Phone.of`, `Review.create`, `RatingStats.average`), sem dependência de controller, service ou repository.

### 1.1. Estrutura de pacotes (como está no código)

```
br.com.clinics
├── ClinicsBackendApplication
├── config/           # ClockConfig (fuso America/Recife), JwtProperties, OpenApiConfig,
│                     #   SiteConfig, SnakeCaseModelConverter, StaticFilesConfig, StorageProperties
├── controller/       # AppDownloadController, AuthController, ClinicController, ContactController,
│                     #   ExamController, ExamTypeController, FavoriteController, MeController,
│                     #   NeighborhoodController, ReviewController, SavingsController, FieldNames
├── dto/              # AuthDtos, ExamDtos, ProfileDtos (requests/responses);
│                     #   AuthenticateCommand, ChangePasswordCommand, RegisterUserCommand,
│                     #   UpdateProfileCommand, SearchExamsQuery; AuthResult, ClinicProfile,
│                     #   ContactableExam, ContactResult, ExamDetails, IssuedToken, PageResult, SubmitResult
├── exception/        # DomainException, AuthenticationFailedException, ConflictException,
│                     #   NotFoundException, ValidationException, GlobalExceptionHandler (RFC 9457)
├── model/            # AvatarImage, Clinic, ContactEvent, Cpf, ExamCategory, ExamOffer, ExamSearchItem,
│                     #   ExamSort, FavoriteExam, GeoPoint, NeighborhoodSummary, OperatingHours,
│                     #   PasswordPolicy, Phone, PublishedReview, RatingStats, Review, ReviewerName,
│                     #   Role, SavingsSummary, TrendingExam, User, UserJpaEntity, WhatsAppLinkBuilder
├── repository/       # ClinicRepository, ContactEventRepository, ExamOfferRepository, FavoriteRepository,
│                     #   ReviewRepository, TrendingExamRepository, UserRepository, UserJpaRepository
├── security/         # SecurityConfig, JwtTokenIssuer, BcryptPasswordHasher, ProblemDetailSecurityHandler
├── service/          # AuthenticateService, ChangePasswordService, ContactableExamLookup,
│                     #   GetClinicProfileService, GetExamDetailsService, GetSavingsService,
│                     #   GetTrendingExamsService, ListNeighborhoodsService, ManageFavoritesService,
│                     #   ManageProfileService, RegisterContactService, RegisterUserService,
│                     #   ReviewClinicService, SearchExamsService, Paging
└── storage/          # LocalAvatarStorage
```

### 1.2. Por que camadas

O backend tem poucas regras de negócio: a maior parte das funcionalidades é ler o catálogo, gravar um contato, uma avaliação ou um favorito, e editar o perfil. Para esse tamanho, camadas são mais simples de navegar e de manter: há um pacote por papel técnico, o caminho de uma requisição é sempre o mesmo e não há interfaces com uma única implementação. As regras que existem continuam em `model`, testáveis sem subir o Spring. A direção das dependências é garantida pelo ArchUnit, então a simplicidade não vira acoplamento.

### 1.3. Regras verificadas pelo ArchUnit

`src/test/java/br/com/clinics/ArchitectureTest.java` falha o build se alguma destas regras for quebrada:

1. **Camadas** (`layeredArchitecture`, considerando só dependências entre as camadas):
   - `controller` não é acessado por nenhuma camada;
   - `service` só é acessado por `controller` e pelo próprio `service`;
   - `repository` só é acessado por `service`.
2. `model` não depende de `controller`, `service` nem `repository`.
3. Não existem mais pacotes `port`, `application`, `infrastructure` ou `domain`.
4. Classes `*Controller` ficam em `controller`; `*Service` em `service`; `*Repository` em `repository`.

### 1.4. Comunicação entre áreas

O registro de contato (engajamento) precisa de dados de uma oferta do catálogo. Em vez de ler as tabelas do catálogo de novo, `ContactableExamLookup` (pacote `service`) reutiliza `GetExamDetailsService.getById` e devolve só o que o contato usa: o record `ContactableExam` (id, nome, preço, economia e WhatsApp). Consultas de leitura que cruzam tabelas de áreas diferentes (ex.: "em alta" lê `contact_events`) usam SQL direto no repositório.

## 2. Diagramas de classes

Os diagramas mostram as classes que existem no código, agrupadas por área funcional. Em cada área há um diagrama das camadas (controller → service → repository) e um do `model`. Os métodos dos controllers são package-private; acessores, construtores e alguns campos descritivos (ex.: `description`, `city`, `logoUrl` em `Clinic`) foram omitidos para legibilidade.

> **Diferenças em relação ao modelo conceitual do MER (doc 05):** no catálogo, `ExamType` + `Exam` aparecem unidos como o objeto de leitura `ExamOffer`, porque a Entrega 2 só lê o catálogo (a escrita de ofertas pertence à visão da clínica, fora do escopo). `PriceHistory` existe como tabela, mas não tem classe em `model` enquanto nada altera preços. Valores monetários usam `BigDecimal` direto.

### 2.1. Identidade (cadastro, login e perfil)

```mermaid
classDiagram
    direction LR
    class AuthController {
        -RegisterUserService register
        -AuthenticateService authenticate
        register(RegisterRequest) AuthResponse
        login(LoginRequest) AuthResponse
    }
    class MeController {
        -ManageProfileService profile
        -ChangePasswordService password
        get(Jwt) ProfileResponse
        update(Jwt, UpdateProfileRequest) ProfileResponse
        avatar(Jwt, MultipartFile) ProfileResponse
        changePassword(Jwt, ChangePasswordRequest)
    }
    class RegisterUserService {
        -UserRepository users
        -BcryptPasswordHasher hasher
        -JwtTokenIssuer tokens
        -Clock clock
        +register(RegisterUserCommand) AuthResult
    }
    class AuthenticateService {
        -UserRepository users
        -BcryptPasswordHasher hasher
        -JwtTokenIssuer tokens
        -String dummyHash
        +authenticate(AuthenticateCommand) AuthResult
    }
    class ManageProfileService {
        -UserRepository users
        -LocalAvatarStorage avatars
        -Clock clock
        +getProfile(UUID) User
        +updateProfile(UpdateProfileCommand) User
        +changeAvatar(UUID, byte[]) User
    }
    class ChangePasswordService {
        -UserRepository users
        -BcryptPasswordHasher hasher
        -Clock clock
        +changePassword(ChangePasswordCommand)
    }
    class UserRepository {
        -UserJpaRepository jpa
        +findByPhone(Phone) Optional~User~
        +existsByPhone(Phone) boolean
        +existsByCpf(Cpf) boolean
        +findById(UUID) Optional~User~
        +existsByEmailExcluding(String, UUID) boolean
        +save(User) User
    }
    class UserJpaRepository {
        <<interface>>
        +findByPhone(String) Optional~UserJpaEntity~
        +existsByPhone(String) boolean
        +existsByCpf(String) boolean
        +existsByEmailIgnoreCaseAndIdNot(String, UUID) boolean
    }
    class JwtTokenIssuer {
        +issue(User) IssuedToken
    }
    class BcryptPasswordHasher {
        +hash(String) String
        +matches(String, String) boolean
    }
    class LocalAvatarStorage {
        +save(UUID, AvatarImage) String
        +delete(String)
    }
    AuthController --> RegisterUserService : UC01
    AuthController --> AuthenticateService : UC02
    MeController --> ManageProfileService : UC11
    MeController --> ChangePasswordService : UC12
    RegisterUserService --> UserRepository
    RegisterUserService --> BcryptPasswordHasher
    RegisterUserService --> JwtTokenIssuer
    AuthenticateService --> UserRepository
    AuthenticateService --> BcryptPasswordHasher
    AuthenticateService --> JwtTokenIssuer
    ManageProfileService --> UserRepository
    ManageProfileService --> LocalAvatarStorage
    ChangePasswordService --> UserRepository
    ChangePasswordService --> BcryptPasswordHasher
    UserRepository --> UserJpaRepository : converte User ⇄ UserJpaEntity
```

```mermaid
classDiagram
    direction LR
    class User {
        -UUID id
        -Phone phone
        -Cpf cpf
        -String email
        -String passwordHash
        -String fullName
        -Role role
        -LocalDate birthDate
        -String avatarUrl
        -Instant createdAt
        -Instant updatedAt
        +register(phone, cpf, fullName, email, birthDate, passwordHash, today, now)$ User
        +updateProfile(newFullName, newEmail, newBirthDate, today, now)
        +changePasswordHash(newHash, now)
        +changeAvatar(newAvatarUrl, now)
    }
    class UserJpaEntity {
        <<entity JPA>>
        -UUID id
        -String phone
        -String cpf
        -String email
        -String passwordHash
        -String fullName
        -Role role
        -LocalDate birthDate
        -String avatarUrl
    }
    class Role {
        <<enumeration>>
        PATIENT
        CLINIC_MANAGER
        ADMIN
    }
    class Phone {
        <<record>>
        +String digits
        +of(raw)$ Phone
        +formatted() String
    }
    class Cpf {
        <<record>>
        +String digits
        +of(raw)$ Cpf
        +formatted() String
    }
    class PasswordPolicy {
        <<utility>>
        +int MIN_LENGTH$
        +validate(rawPassword)$
    }
    class AvatarImage {
        <<record>>
        +byte[] bytes
        +String extension
        +String contentType
        +int MAX_BYTES$
        +of(data)$ AvatarImage
    }
    User --> Role
    User *-- Phone
    User *-- Cpf
    User ..> PasswordPolicy : validada no service antes do cadastro
    User ..> AvatarImage : foto validada antes de gravar
    UserJpaEntity ..> User : mapeado pelo UserRepository
```

### 2.2. Catálogo (busca, detalhes, clínica, bairros e em alta)

```mermaid
classDiagram
    direction LR
    class ExamController {
        -SearchExamsService search
        -GetExamDetailsService details
        search(query, category, sortBy, lat, lng, page, size, neighborhood, minPrice, maxPrice, minRating, bestPriceOnly) SearchResponse
        details(UUID, Double, Double) ExamDetailResponse
    }
    class ClinicController {
        -GetClinicProfileService profile
        get(UUID) ClinicProfileResponse
    }
    class ExamTypeController {
        -GetTrendingExamsService trending
        trending() List~TrendingResponse~
    }
    class NeighborhoodController {
        -ListNeighborhoodsService neighborhoods
        list() List~NeighborhoodResponse~
    }
    class SearchExamsService {
        -ExamOfferRepository offers
        +search(SearchExamsQuery) PageResult~ExamSearchItem~
    }
    class GetExamDetailsService {
        -ExamOfferRepository offers
        -ClinicRepository clinics
        -Clock clock
        +getById(UUID, Double, Double) ExamDetails
    }
    class GetClinicProfileService {
        -ClinicRepository clinics
        -ExamOfferRepository offers
        -Clock clock
        +getById(UUID) ClinicProfile
    }
    class GetTrendingExamsService {
        -TrendingExamRepository trending
        -Clock clock
        +getTrending() List~TrendingExam~
    }
    class ListNeighborhoodsService {
        -ClinicRepository clinics
        +list() List~NeighborhoodSummary~
    }
    class ExamOfferRepository {
        -JdbcClient jdbc
        +findAvailable(String, ExamCategory) List~ExamOffer~
        +findAvailableById(UUID) Optional~ExamOffer~
        +findAvailableByClinic(UUID) List~ExamOffer~
    }
    class ClinicRepository {
        -JdbcClient jdbc
        +findById(UUID) Optional~Clinic~
        +listNeighborhoods() List~NeighborhoodSummary~
    }
    class TrendingExamRepository {
        -JdbcClient jdbc
        +findTrending(Instant, int) List~TrendingExam~
    }
    ExamController --> SearchExamsService : UC03
    ExamController --> GetExamDetailsService : UC04
    ClinicController --> GetClinicProfileService : UC05
    ExamTypeController --> GetTrendingExamsService : UC06
    NeighborhoodController --> ListNeighborhoodsService
    SearchExamsService --> ExamOfferRepository
    GetExamDetailsService --> ExamOfferRepository
    GetExamDetailsService --> ClinicRepository
    GetClinicProfileService --> ClinicRepository
    GetClinicProfileService --> ExamOfferRepository
    GetTrendingExamsService --> TrendingExamRepository
    ListNeighborhoodsService --> ClinicRepository
```

```mermaid
classDiagram
    direction LR
    class ExamOffer {
        <<record>>
        +UUID examId
        +UUID examTypeId
        +String examName
        +ExamCategory category
        +UUID clinicId
        +String clinicName
        +String neighborhood
        +GeoPoint clinicLocation
        +BigDecimal clinicRating
        +boolean clinicVerified
        +String whatsappNumber
        +BigDecimal price
        +BigDecimal averagePrice
        +BigDecimal minPrice
        +String preparation
        +String paymentMethods
        +savings() BigDecimal
        +isBestPrice() boolean
        +distanceKmFrom(origin) Double
    }
    class Clinic {
        <<record>>
        +UUID id
        +String name
        +String address
        +String neighborhood
        +GeoPoint location
        +String whatsappNumber
        +BigDecimal rating
        +int reviewCount
        +boolean verified
        +List~OperatingHours~ operatingHours
        +isOpenAt(ZonedDateTime) boolean
    }
    class OperatingHours {
        <<record>>
        +DayOfWeek dayOfWeek
        +LocalTime opensAt
        +LocalTime closesAt
        +covers(ZonedDateTime) boolean
    }
    class GeoPoint {
        <<record>>
        +double latitude
        +double longitude
        +distanceKmTo(other) double
    }
    class ExamCategory {
        <<enumeration>>
        SANGUE
        RAIO_X
        CONSULTAS
        CARDIO
        ULTRASSOM
        +label() String
        +parse(value)$ ExamCategory
    }
    class ExamSort {
        <<enumeration>>
        PRICE
        RATING
        NAME
        DISTANCE
        +parse(value)$ ExamSort
        +comparator() Comparator
    }
    class ExamSearchItem {
        <<record>>
        +ExamOffer offer
        +Double distanceKm
    }
    class TrendingExam {
        <<record>>
        +UUID examTypeId
        +String name
        +String slug
        +ExamCategory category
        +BigDecimal minPrice
        +int offersCount
        +long recentContacts
    }
    class NeighborhoodSummary {
        <<record>>
        +String name
        +int clinicsCount
    }
    class ExamDetails {
        <<record dto>>
        +ExamOffer offer
        +Clinic clinic
        +boolean clinicOpenNow
        +Double distanceKm
    }
    class ClinicProfile {
        <<record dto>>
        +Clinic clinic
        +boolean openNow
        +List~ExamOffer~ offers
    }
    ExamOffer --> ExamCategory
    ExamOffer o-- GeoPoint : localização da clínica
    Clinic *-- GeoPoint
    Clinic "1" *-- "0..*" OperatingHours
    ExamSearchItem *-- ExamOffer
    ExamSort ..> ExamSearchItem : ordena
    TrendingExam --> ExamCategory
    ExamDetails *-- ExamOffer
    ExamDetails *-- Clinic
    ClinicProfile *-- Clinic
    ClinicProfile o-- ExamOffer
```

### 2.3. Engajamento (contato, economia, favoritos e avaliações)

```mermaid
classDiagram
    direction LR
    class ContactController {
        -RegisterContactService contact
        register(UUID, Jwt) ContactResponse
    }
    class SavingsController {
        -GetSavingsService savings
        get(Jwt) SavingsResponse
    }
    class FavoriteController {
        -ManageFavoritesService favorites
        add(Jwt, UUID)
        remove(Jwt, UUID)
        list(Jwt, Integer, Integer) FavoritesResponse
    }
    class ReviewController {
        -ReviewClinicService reviews
        submit(Jwt, UUID, ReviewRequest) ResponseEntity~MyReviewResponse~
        mine(Jwt, UUID) MyReviewResponse
        list(UUID, Integer, Integer) ReviewsResponse
    }
    class RegisterContactService {
        -ContactableExamLookup exams
        -ContactEventRepository events
        -Clock clock
        +register(UUID examId, UUID userId) ContactResult
    }
    class ContactableExamLookup {
        -GetExamDetailsService catalog
        +findById(UUID) Optional~ContactableExam~
    }
    class GetExamDetailsService {
        +getById(UUID, Double, Double) ExamDetails
    }
    class GetSavingsService {
        -ContactEventRepository events
        +getSavings(UUID) SavingsSummary
    }
    class ManageFavoritesService {
        -FavoriteRepository favorites
        +add(UUID, UUID)
        +remove(UUID, UUID)
        +list(UUID, Integer, Integer) PageResult~FavoriteExam~
    }
    class ReviewClinicService {
        -ReviewRepository reviews
        -Clock clock
        +submit(UUID userId, UUID clinicId, int rating, String comment) SubmitResult
        +list(UUID, Integer, Integer) PageResult~PublishedReview~
        +getMine(UUID, UUID) Review
    }
    class ContactEventRepository {
        -JdbcClient jdbc
        +save(ContactEvent)
        +summarizeByUser(UUID) SavingsSummary
    }
    class FavoriteRepository {
        -JdbcClient jdbc
        +examTypeExists(UUID) boolean
        +add(UUID, UUID)
        +remove(UUID, UUID)
        +findByUser(UUID, int, int) List~FavoriteExam~
        +countByUser(UUID) long
    }
    class ReviewRepository {
        -JdbcClient jdbc
        +clinicExists(UUID) boolean
        +findByUserAndClinic(UUID, UUID) Optional~Review~
        +save(Review)
        +statsFor(UUID) RatingStats
        +updateClinicRating(UUID, RatingStats)
        +findByClinic(UUID, int, int) List~PublishedReview~
        +countByClinic(UUID) long
    }
    ContactController --> RegisterContactService : UC07
    SavingsController --> GetSavingsService : UC08
    FavoriteController --> ManageFavoritesService : UC09
    ReviewController --> ReviewClinicService : UC10
    RegisterContactService --> ContactableExamLookup
    RegisterContactService --> ContactEventRepository
    ContactableExamLookup --> GetExamDetailsService : reutiliza o catálogo
    GetSavingsService --> ContactEventRepository
    ManageFavoritesService --> FavoriteRepository
    ReviewClinicService --> ReviewRepository
```

```mermaid
classDiagram
    direction LR
    class Review {
        -UUID id
        -UUID clinicId
        -UUID userId
        -int rating
        -String comment
        -Instant createdAt
        -Instant updatedAt
        +int MAX_COMMENT_LENGTH$
        +create(clinicId, userId, rating, comment, now)$ Review
        +edit(newRating, newComment, now)
    }
    class RatingStats {
        <<record>>
        +long sum
        +long count
        +average() BigDecimal
    }
    class ReviewerName {
        <<utility>>
        +abbreviate(fullName)$ String
    }
    class PublishedReview {
        <<record>>
        +Review review
        +String authorName
    }
    class FavoriteExam {
        <<record>>
        +UUID examTypeId
        +String name
        +String slug
        +ExamCategory category
        +BigDecimal minPrice
        +int offersCount
        +Instant favoritedAt
    }
    class ContactEvent {
        <<record>>
        +UUID id
        +UUID examId
        +UUID userId
        +BigDecimal priceAtContact
        +BigDecimal estimatedSavings
        +Instant createdAt
        +of(examId, userId, price, savings, now)$ ContactEvent
    }
    class WhatsAppLinkBuilder {
        <<utility>>
        +build(whatsappNumber, examName, price)$ URI
    }
    class SavingsSummary {
        <<record>>
        +BigDecimal totalSavings
        +long contactsCount
        +empty()$ SavingsSummary
    }
    class ContactableExam {
        <<record dto>>
        +UUID examId
        +String examName
        +BigDecimal price
        +BigDecimal savings
        +String whatsappNumber
    }
    class ContactResult {
        <<record dto>>
        +UUID contactId
        +URI whatsappUrl
        +BigDecimal estimatedSavings
    }
    class SubmitResult {
        <<record dto>>
        +Review review
        +boolean created
    }
    PublishedReview *-- Review
    PublishedReview ..> ReviewerName : abrevia o autor
    Review ..> RatingStats : base da nota média
    SubmitResult *-- Review
    ContactEvent ..> ContactableExam : preço e economia congelados
    ContactResult ..> WhatsAppLinkBuilder : whatsappUrl
    SavingsSummary ..> ContactEvent : soma
```

### 2.4. Exceções

```mermaid
classDiagram
    direction LR
    class DomainException {
        <<abstract>>
        -String code
        +code() String
    }
    class ValidationException
    class ConflictException
    class NotFoundException
    class AuthenticationFailedException
    DomainException <|-- ValidationException : HTTP 400
    DomainException <|-- ConflictException : HTTP 409
    DomainException <|-- NotFoundException : HTTP 404
    DomainException <|-- AuthenticationFailedException : HTTP 401
```

O `GlobalExceptionHandler` converte cada subtipo em `application/problem+json` com o `code` da exceção. Erros 401 e 403 do Spring Security saem no mesmo formato pelo `ProblemDetailSecurityHandler`.

## 3. Controllers e services

| Controller | Service | UC (doc de requisitos) |
|---|---|---|
| `AuthController` | `RegisterUserService`, `AuthenticateService` | UC01, UC02 |
| `MeController` | `ManageProfileService`, `ChangePasswordService` | UC11, UC12 |
| `ExamController` | `SearchExamsService`, `GetExamDetailsService` | UC03, UC04 |
| `ClinicController` | `GetClinicProfileService` | UC05 |
| `ExamTypeController` | `GetTrendingExamsService` | UC06 |
| `NeighborhoodController` | `ListNeighborhoodsService` | filtro de bairros da busca |
| `ContactController` | `RegisterContactService` | UC07 |
| `SavingsController` | `GetSavingsService` | UC08 |
| `FavoriteController` | `ManageFavoritesService` | UC09 |
| `ReviewController` | `ReviewClinicService` | UC10 |
| `AppDownloadController` | (nenhum; lê o APK do disco) | download do app Android |

## 4. Decisões técnicas e bibliotecas

| Preocupação | Escolha | Justificativa |
|---|---|---|
| Framework | Spring Boot 4.1.1, Java 25 | Plataforma do curso; Java 25 é LTS |
| Persistência de escrita | Spring Data JPA + Hibernate | Padrão de mercado para agregados simples (`users`) |
| Consultas de leitura | `JdbcClient` com SQL nativo | Funções de janela (média e mínimo por exame), `unaccent`, agregações; JPQL não expressa isso bem |
| Migrações | Flyway | Esquema versionado e reprodutível; Hibernate só valida (`ddl-auto=validate`) |
| JWT | Spring Security OAuth2 Resource Server (Nimbus) | Validação mantida pelo Spring Security, sem biblioteca extra |
| Senhas | `BCryptPasswordEncoder`, custo 10 | RNF01 |
| Validação | Jakarta Bean Validation + regras em `model` | Formato nos DTOs; regras de negócio nas classes de `model` |
| DTOs | `record` do Java | Imutáveis e concisos |
| Documentação da API | springdoc-openapi 3.1 | Swagger UI e contrato em `/v3/api-docs` |
| Erros | `ProblemDetail` (RFC 9457) + `@RestControllerAdvice` | RNF07 |
| Testes | JUnit 5, AssertJ, MockMvc, Testcontainers, ArchUnit | RNF08 |
| Observabilidade | Actuator (`/actuator/health`) | Health check para o deploy |
| Infra local | Docker Compose (PostgreSQL 17) | RNF10 |

### Por que JPA e JDBC juntos
`users` é um agregado pequeno com ciclo de vida de criação e edição, bem atendido por JPA: `UserRepository` usa o `UserJpaRepository` (Spring Data) e converte `UserJpaEntity` em `User`, então os services nunca veem a entidade JPA. As consultas do catálogo e do engajamento são de leitura, agregam entre tabelas e usam recursos do PostgreSQL; escrevê-las em SQL (`JdbcClient`) deixa a intenção explícita e evita projeções JPA artificiais. Os services só conhecem as classes `*Repository`, não a tecnologia por trás delas.

## 5. Estratégia de testes

| Nível | O que cobre | Ferramenta |
|---|---|---|
| Unidade | Regras de negócio puras em `model` (RN03, RN14, RN15, RN19, RN22, RN25–RN28, RN34) | JUnit 5 + AssertJ, sem Spring |
| Integração | Cada endpoint com a aplicação completa e PostgreSQL real | `@SpringBootTest` + MockMvc + Testcontainers |
| Arquitetura | Direção controller → service → repository e nomes das classes (seção 1.3) | ArchUnit |
| Contrato | Endpoints presentes no OpenAPI e nomes em `snake_case` | MockMvc em `/v3/api-docs` |

Os testes de integração criam os próprios usuários com telefone aleatório e, quando alteram o seed, restauram o estado ao final.

## 6. Histórico

O projeto começou com arquitetura hexagonal organizada por módulos. Em outubro de 2026 foi migrado para camadas, para simplificar a navegação e a manutenção. O contrato da API (rotas, JSON e códigos de erro) não mudou.
