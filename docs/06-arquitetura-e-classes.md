# 06 · Arquitetura e Diagrama de Classes

## 1. Estilo arquitetural

**Arquitetura hexagonal (portas e adaptadores), organizada por funcionalidade** (*package by feature*). O domínio não depende de Spring, JPA nem HTTP; as dependências apontam sempre para dentro.

```mermaid
flowchart LR
    subgraph IN["Adaptadores de entrada"]
        REST["Controllers REST<br/>(DTOs + validação)"]
    end
    subgraph APP["Aplicação"]
        UCS["Casos de uso<br/>(services @Transactional)"]
        PIN["Portas de entrada<br/>(interfaces)"]
        POUT["Portas de saída<br/>(repositórios, storage, token)"]
    end
    subgraph DOM["Domínio"]
        ENT["Entidades, objetos de valor<br/>e regras de negócio"]
    end
    subgraph OUT["Adaptadores de saída"]
        JPA["JPA e JDBC<br/>(PostgreSQL)"]
        SEC["JWT / BCrypt"]
        STO["Armazenamento de arquivos"]
    end
    REST --> PIN
    PIN -. implementado por .-> UCS
    UCS --> ENT
    UCS --> POUT
    JPA -. implementa .-> POUT
    SEC -. implementa .-> POUT
    STO -. implementa .-> POUT
```

**Por que esta arquitetura:** as regras de negócio (RN14, RN15, RN19, RN22, RN27, RN28) ficam em classes puras, testáveis sem subir o Spring; trocar o armazenamento de fotos (disco → S3) afeta só um adaptador.

### Estrutura de pacotes (como está no código)

```
br.com.clinics
├── shared/
│   ├── domain/                   # DomainException e subtipos, PageResult
│   └── infrastructure/
│       ├── web/                  #   GlobalExceptionHandler (RFC 9457), FieldNames
│       ├── openapi/              #   OpenApiConfig, SnakeCaseModelConverter
│       └── ClockConfig           #   Clock injetável (fuso America/Recife)
├── identity/                     # UC01, UC02, UC11, UC12
│   ├── domain/                   #   User, Phone, Role, PasswordPolicy, AvatarImage
│   ├── application/              #   RegisterUser, Authenticate, ManageProfile, ChangePassword
│   │   └── port/{in,out}/        #   UserRepository, PasswordHasher, TokenIssuer, AvatarStorage
│   └── infrastructure/
│       ├── persistence/          #   UserJpaEntity, SpringDataUserRepository, UserPersistenceAdapter
│       ├── security/             #   SecurityConfig, JwtTokenIssuer, BcryptPasswordHasher, JwtProperties
│       ├── storage/              #   LocalAvatarStorage, StaticFilesConfig
│       └── web/                  #   AuthController, MeController, DTOs
├── catalog/                      # UC03, UC04, UC05, UC06
│   ├── domain/                   #   ExamOffer, Clinic, OperatingHours, GeoPoint, ExamCategory, ExamSort, TrendingExam
│   ├── application/              #   SearchExams, GetExamDetails, GetClinicProfile, GetTrendingExams
│   └── infrastructure/
│       ├── persistence/          #   consultas SQL (JdbcClient): ExamOffer, Clinic, Trending
│       └── web/                  #   ExamController, ExamTypeController, ClinicController
├── engagement/                   # UC07, UC08, UC09, UC10
│   ├── domain/                   #   Review, RatingStats, ReviewerName, ContactEvent, WhatsAppLinkBuilder, ...
│   ├── application/              #   RegisterContact, GetSavings, ManageFavorites, ReviewClinic
│   └── infrastructure/
│       ├── persistence/          #   JDBC + CatalogContactableExamLookup (adaptador anticorrupção)
│       └── web/                  #   ContactController, SavingsController, FavoriteController, ReviewController
└── ClinicsBackendApplication
```

**Regras de dependência**, verificadas automaticamente por `ArchitectureTest` (ArchUnit):
1. `domain` não importa Spring, JPA, Hibernate nem Jackson.
2. `domain` não depende de `application` nem de `infrastructure`.
3. `application` não depende de `infrastructure`.

**Comunicação entre módulos:** `engagement` precisa de dados de uma oferta, mas não importa as classes do catálogo. Ele define a própria porta de saída (`ContactableExamLookup`) e o adaptador `CatalogContactableExamLookup` chama a porta pública `GetExamDetailsUseCase` do catálogo, traduzindo para o vocabulário do `engagement`. Consultas de leitura que cruzam tabelas de módulos diferentes (ex.: "em alta" lê `contact_events`) usam SQL direto, como modelos de leitura.

## 2. Diagrama de classes do domínio

O diagrama mostra as classes que existem no código, agrupadas por módulo.

> **Diferenças em relação ao modelo conceitual do MER (doc 05):** no catálogo, `ExamType` + `Exam` aparecem unidos como o objeto de leitura `ExamOffer`, porque a Entrega 2 só lê o catálogo (a escrita de ofertas pertence à visão da clínica, fora do escopo). `PriceHistory` existe como tabela, mas não tem classe de domínio enquanto nada altera preços. Valores monetários usam `BigDecimal` direto.

### 2.1. Módulo `identity`

```mermaid
classDiagram
    direction LR
    class User {
        -UUID id
        -Phone phone
        -String email
        -String passwordHash
        -String fullName
        -Role role
        -LocalDate birthDate
        -String avatarUrl
        +register(phone, fullName, hash, now)$ User
        +updateProfile(name, email, birthDate, today, now)
        +changePasswordHash(newHash, now)
        +changeAvatar(url, now)
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
    class PasswordPolicy {
        <<utility>>
        +validate(rawPassword)$
    }
    class AvatarImage {
        <<record>>
        +byte[] bytes
        +String extension
        +String contentType
        +of(bytes)$ AvatarImage
    }
    User --> Role
    User *-- Phone
    User ..> PasswordPolicy : usa no cadastro
    User ..> AvatarImage : foto validada antes de gravar
```

### 2.2. Módulo `catalog`

```mermaid
classDiagram
    direction LR
    class ExamOffer {
        <<record>>
        +UUID examId
        +String examName
        +ExamCategory category
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
        +GeoPoint location
        +String whatsappNumber
        +BigDecimal rating
        +int reviewCount
        +boolean verified
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
        +parse(text)$ ExamCategory
    }
    class ExamSort {
        <<enumeration>>
        PRICE
        RATING
        NAME
        DISTANCE
        +parse(text)$ ExamSort
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
        +BigDecimal minPrice
        +int offersCount
        +long recentContacts
    }
    ExamOffer --> ExamCategory
    ExamOffer o-- GeoPoint : localização da clínica
    Clinic *-- GeoPoint
    Clinic "1" *-- "0..*" OperatingHours
    ExamSearchItem *-- ExamOffer
    ExamSort ..> ExamSearchItem : ordena
    TrendingExam --> ExamCategory
```

### 2.3. Módulo `engagement`

```mermaid
classDiagram
    direction LR
    class Review {
        -UUID id
        -UUID clinicId
        -UUID userId
        -int rating
        -String comment
        +create(clinicId, userId, rating, comment, now)$ Review
        +edit(rating, comment, now)
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
    }
    PublishedReview *-- Review
    PublishedReview ..> ReviewerName : abrevia o autor
    Review ..> RatingStats : base da nota média
    ContactEvent ..> WhatsAppLinkBuilder : gerado junto com o link
    SavingsSummary ..> ContactEvent : soma
```

### 2.4. Exceções de domínio

```mermaid
classDiagram
    direction LR
    class DomainException {
        <<abstract>>
        +String code()
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

O `GlobalExceptionHandler` converte cada subtipo em `application/problem+json` com o `code` da exceção.

## 3. Casos de uso e controllers

```mermaid
classDiagram
    direction LR
    class AuthController
    class MeController
    class ExamController
    class ExamTypeController
    class ClinicController
    class ContactController
    class SavingsController
    class FavoriteController
    class ReviewController

    class RegisterUserUseCase { <<interface>> }
    class AuthenticateUseCase { <<interface>> }
    class ManageProfileUseCase { <<interface>> }
    class ChangePasswordUseCase { <<interface>> }
    class SearchExamsUseCase { <<interface>> }
    class GetExamDetailsUseCase { <<interface>> }
    class GetClinicProfileUseCase { <<interface>> }
    class GetTrendingExamsUseCase { <<interface>> }
    class RegisterContactUseCase { <<interface>> }
    class GetSavingsUseCase { <<interface>> }
    class ManageFavoritesUseCase { <<interface>> }
    class ReviewClinicUseCase { <<interface>> }

    AuthController --> RegisterUserUseCase : UC01
    AuthController --> AuthenticateUseCase : UC02
    MeController --> ManageProfileUseCase : UC11
    MeController --> ChangePasswordUseCase : UC12
    ExamController --> SearchExamsUseCase : UC03
    ExamController --> GetExamDetailsUseCase : UC04
    ClinicController --> GetClinicProfileUseCase : UC05
    ExamTypeController --> GetTrendingExamsUseCase : UC06
    ContactController --> RegisterContactUseCase : UC07
    SavingsController --> GetSavingsUseCase : UC08
    FavoriteController --> ManageFavoritesUseCase : UC09
    ReviewController --> ReviewClinicUseCase : UC10
```

## 4. Decisões técnicas e bibliotecas

| Preocupação | Escolha | Justificativa |
|---|---|---|
| Framework | Spring Boot 4.1.1, Java 25 | Plataforma do curso; Java 25 é LTS |
| Persistência de escrita | Spring Data JPA + Hibernate | Padrão de mercado para agregados simples (`users`) |
| Consultas de leitura | `JdbcClient` com SQL nativo | Funções de janela (média e mínimo por exame), `unaccent`, agregações; JPQL não expressa isso bem |
| Migrações | Flyway | Esquema versionado e reprodutível; Hibernate só valida (`ddl-auto=validate`) |
| JWT | Spring Security OAuth2 Resource Server (Nimbus) | Validação mantida pelo Spring Security, sem biblioteca extra |
| Senhas | `BCryptPasswordEncoder`, custo 10 | RNF01 |
| Validação | Jakarta Bean Validation + regras no domínio | Formato nos DTOs; regras de negócio nas entidades |
| DTOs | `record` do Java | Imutáveis e concisos |
| Documentação da API | springdoc-openapi 3.1 | Swagger UI e contrato em `/v3/api-docs` |
| Erros | `ProblemDetail` (RFC 9457) + `@RestControllerAdvice` | RNF07 |
| Testes | JUnit 5, AssertJ, MockMvc, Testcontainers, ArchUnit | RNF08 |
| Observabilidade | Actuator (`/actuator/health`) | Health check para o deploy |
| Infra local | Docker Compose (PostgreSQL 17) | RNF10 |

### Por que JPA e JDBC juntos
`users` é um agregado pequeno com ciclo de vida de criação e edição, bem atendido por JPA. As consultas do catálogo e do engajamento são de leitura, agregam entre tabelas e usam recursos do PostgreSQL; escrevê-las em SQL deixa a intenção explícita e evita projeções JPA artificiais. Ambas ficam atrás de portas de saída, então o restante do sistema não sabe qual tecnologia é usada.

## 5. Estratégia de testes

| Nível | O que cobre | Ferramenta |
|---|---|---|
| Unidade | Regras de negócio puras (RN03, RN14, RN15, RN19, RN22, RN25–RN28, RN34) | JUnit 5 + AssertJ, sem Spring |
| Integração | Cada endpoint com a aplicação completa e PostgreSQL real | `@SpringBootTest` + MockMvc + Testcontainers |
| Arquitetura | Regras de dependência entre camadas | ArchUnit |
| Contrato | Endpoints presentes no OpenAPI e nomes em `snake_case` | MockMvc em `/v3/api-docs` |

Os testes de integração criam os próprios usuários com telefone aleatório e, quando alteram o seed, restauram o estado ao final.
