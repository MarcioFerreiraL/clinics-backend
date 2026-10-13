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
        POUT["Portas de saída<br/>(repositórios)"]
    end
    subgraph DOM["Domínio"]
        ENT["Entidades, objetos de valor<br/>e serviços de domínio"]
    end
    subgraph OUT["Adaptadores de saída"]
        JPA["Repositórios JPA<br/>(PostgreSQL)"]
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

**Por que esta arquitetura:** regras de negócio (RN14, RN15, RN19, RN22, RN28) ficam em classes puras e testáveis sem subir o Spring; trocar o armazenamento de fotos (disco → S3) afeta só um adaptador.

### Estrutura de pacotes

```
br.com.clinics
├── shared/                       # exceções de domínio, ProblemDetail, paginação, relógio (Clock)
├── identity/                     # UC01, UC02, UC11, UC12
│   ├── domain/                   #   User, Role, Phone, ...
│   ├── application/              #   RegisterUser, Authenticate, UpdateProfile, ChangePassword
│   │   └── port/{in,out}/
│   └── infrastructure/
│       ├── web/                  #   AuthController, MeController, DTOs
│       ├── persistence/          #   UserJpaEntity, SpringDataUserRepository, mapper
│       └── security/             #   JwtService, SecurityConfig
├── catalog/                      # UC03, UC04, UC05, UC06
│   ├── domain/                   #   Clinic, ExamType, Exam, OperatingHours, PriceComparison, GeoPoint
│   ├── application/
│   └── infrastructure/
├── engagement/                   # UC07, UC08, UC09, UC10
│   ├── domain/                   #   Favorite, Review, ContactEvent, WhatsAppLink
│   ├── application/
│   └── infrastructure/
└── ClinicsBackendApplication
```

**Regras de dependência (verificadas por teste ArchUnit):**
1. `domain` não importa nada de `application`, `infrastructure`, Spring ou JPA.
2. `application` só conhece `domain` e as próprias portas.
3. Um módulo não acessa as classes JPA de outro. A comunicação é por portas.

## 2. Diagrama de classes do domínio

```mermaid
classDiagram
    direction LR

    class User {
        +UUID id
        +Phone phone
        +String email
        +String passwordHash
        +String fullName
        +Role role
        +LocalDate birthDate
        +String avatarUrl
        +changePassword(current, next, encoder)
        +updateProfile(name, email, birthDate)
    }
    class Role {
        <<enumeration>>
        PATIENT
        CLINIC_MANAGER
        ADMIN
    }
    class Phone {
        <<value object>>
        +String digits
        +of(raw) Phone
        +formatted() String
    }

    class Clinic {
        +UUID id
        +String name
        +String description
        +String address
        +String neighborhood
        +GeoPoint location
        +String whatsappNumber
        +BigDecimal rating
        +int reviewCount
        +int patientCount
        +boolean verified
        +isOpenAt(ZonedDateTime) boolean
        +applyReviewStats(avg, count)
    }
    class OperatingHours {
        +DayOfWeek dayOfWeek
        +LocalTime opensAt
        +LocalTime closesAt
        +covers(ZonedDateTime) boolean
    }
    class GeoPoint {
        <<value object>>
        +BigDecimal latitude
        +BigDecimal longitude
        +distanceKmTo(GeoPoint) double
    }

    class ExamType {
        +UUID id
        +String name
        +String slug
        +ExamCategory category
        +String description
    }
    class ExamCategory {
        <<enumeration>>
        SANGUE
        RAIO_X
        CONSULTAS
        CARDIO
        ULTRASSOM
    }
    class Exam {
        +UUID id
        +Money price
        +String preparation
        +String paymentMethods
        +boolean available
        +changePrice(Money) PriceHistoryEntry
    }
    class Money {
        <<value object>>
        +BigDecimal amount
        +of(BigDecimal) Money
        +minus(Money) Money
    }
    class PriceHistoryEntry {
        +UUID id
        +Money price
        +Instant recordedAt
    }

    class Favorite {
        +Instant createdAt
    }
    class Review {
        +UUID id
        +int rating
        +String comment
        +edit(rating, comment)
    }
    class ContactEvent {
        +UUID id
        +Money priceAtContact
        +Money estimatedSavings
        +Instant createdAt
    }

    class PriceComparison {
        <<domain service>>
        +average(List~Money~) Money
        +savings(Money price, Money avg) Money
        +bestPrice(List~Exam~) List~Exam~
    }
    class WhatsAppLinkBuilder {
        <<domain service>>
        +build(clinic, exam) URI
    }

    User --> Role
    User *-- Phone
    Clinic *-- GeoPoint
    Clinic "1" *-- "0..*" OperatingHours
    ExamType --> ExamCategory
    ExamType "1" o-- "0..*" Exam : ofertado como
    Clinic "1" o-- "0..*" Exam : oferta
    Exam *-- Money
    Exam "1" *-- "0..*" PriceHistoryEntry
    User "1" -- "0..*" Favorite
    ExamType "1" -- "0..*" Favorite
    User "1" -- "0..*" Review
    Clinic "1" -- "0..*" Review
    Exam "1" -- "0..*" ContactEvent
    User "0..1" -- "0..*" ContactEvent
    PriceComparison ..> Exam
    PriceComparison ..> Money
    WhatsAppLinkBuilder ..> Clinic
    WhatsAppLinkBuilder ..> Exam
```

**Associações:** composição (`*--`) para partes que não existem sem o todo (`OperatingHours`, `PriceHistoryEntry`); agregação (`o--`) para ofertas, que pertencem à clínica mas têm ciclo de vida próprio.

## 3. Diagrama de componentes dos casos de uso

```mermaid
classDiagram
    direction TB
    class AuthController
    class MeController
    class ExamController
    class ClinicController
    class EngagementController

    class RegisterUserUseCase { <<interface>> }
    class AuthenticateUseCase { <<interface>> }
    class SearchExamsUseCase { <<interface>> }
    class GetExamDetailsUseCase { <<interface>> }
    class GetClinicProfileUseCase { <<interface>> }
    class GetTrendingExamsUseCase { <<interface>> }
    class RegisterContactUseCase { <<interface>> }
    class ManageFavoritesUseCase { <<interface>> }
    class ReviewClinicUseCase { <<interface>> }
    class ManageProfileUseCase { <<interface>> }

    AuthController --> RegisterUserUseCase
    AuthController --> AuthenticateUseCase
    MeController --> ManageProfileUseCase
    MeController --> ManageFavoritesUseCase
    ExamController --> SearchExamsUseCase
    ExamController --> GetExamDetailsUseCase
    ExamController --> GetTrendingExamsUseCase
    ExamController --> RegisterContactUseCase
    ClinicController --> GetClinicProfileUseCase
    EngagementController --> ReviewClinicUseCase
```

## 4. Decisões técnicas e bibliotecas

| Preocupação | Escolha | Justificativa |
|---|---|---|
| Framework | Spring Boot 4.1.1, Java 25 | Plataforma-alvo do curso; Java 25 é LTS |
| Persistência | Spring Data JPA + Hibernate | Padrão de mercado; consultas complexas via JPQL ou SQL nativo |
| Migrações | Flyway | Esquema versionado e reprodutível (RNF05) |
| JWT | Spring Security OAuth2 Resource Server (Nimbus) | Validação mantida pelo Spring Security; evita biblioteca extra e superfície de ataque |
| Senhas | BCrypt (`BCryptPasswordEncoder`, custo 10) | RNF01 |
| Validação | Jakarta Bean Validation | Validação declarativa nos DTOs |
| Mapeamento | Conversores manuais (mappers) entre JPA, domínio e DTO | Sem mágica de reflexão e fácil de testar. MapStruct é opção se o volume crescer |
| DTOs | `record` do Java | Imutáveis e concisos |
| Documentação da API | springdoc-openapi | RNF06 |
| Erros | `ProblemDetail` (RFC 9457) + `@RestControllerAdvice` | RNF07 |
| Testes | JUnit 5, AssertJ, Mockito, Testcontainers, ArchUnit | RNF08 e regras de dependência |
| Observabilidade | Actuator (`/health`, `/info`) | Health check para deploy |

## 5. Estratégia de testes

| Nível | O que cobre | Ferramenta |
|---|---|---|
| Unidade | Regras de negócio puras (RN14, RN15, RN19, RN22, RN28) | JUnit + AssertJ, sem Spring |
| Aplicação | Casos de uso com portas simuladas | Mockito |
| Integração | Repositórios e consultas com PostgreSQL real | Testcontainers |
| Web | Controllers, segurança, validação e formato de erro | `MockMvc` |
| Arquitetura | Regras de dependência entre camadas | ArchUnit |
