# 10 · Testes e rastreabilidade

Os testes nascem dos documentos: cada requisito (RF/RNF), regra de negócio (RN) e restrição do modelo de dados (MER) aponta para pelo menos um teste. O nome (`@DisplayName`) de cada teste começa com o identificador que ele cobre, então dá para procurar por `RN15`, `RF08` etc. no código e nos relatórios.

## Camadas

| Camada | Onde | O que verifica | Roda |
|---|---|---|---|
| Unidade (domínio) | `*/domain/*Test` | Regras puras: CPF, telefone, senha, distância, abertura da clínica, nota, link do WhatsApp | CI |
| Integração por endpoint | `*IntegrationTest` | Cada caso de uso pela API, com PostgreSQL real (Testcontainers) | CI |
| **Modelo de dados (MER)** | `model/DataModelIntegrationTest` | Tabelas, tipos, `UNIQUE`, `CHECK`, FKs e `ON DELETE` do `05-modelo-de-dados.md` | CI |
| **Consistência do seed** | `rules/SeedDataConsistencyIntegrationTest` | Os dados de demonstração respeitam as regras (WhatsApp só com dígitos, nota = média das avaliações, histórico de preço, apelidos…) | CI |
| **Regras de negócio** | `rules/BusinessRulesIntegrationTest` | RN05, 06, 07, 12, 15, 17, 19, 20, 21, 24, 30, 35, 36, 38 e RNF04, 06, 07 pela API | CI |
| **Jornada do paciente** | `journey/PatientJourneyIntegrationTest` | RF01 a RF12 de ponta a ponta, mais a jornada do visitante e o texto do WhatsApp (RN28) | CI |
| Arquitetura | `ArchitectureTest` | Domínio sem Spring/JPA; módulos só se falam pelas portas | CI |
| Contrato | `OpenApiIntegrationTest` | Endpoints documentados no OpenAPI | CI |
| **Smoke pós-deploy** | `deploy/smoke-test.sh` | A API publicada na VPS responde conforme o contrato | CD |

Os testes de integração que gravam dados rodam em transação desfeita no fim (`@Transactional`), então não interferem uns nos outros.

## Matriz de rastreabilidade

### Requisitos funcionais

| Requisito | Teste principal |
|---|---|
| RF01 cadastro | `PatientJourney` (passo 1), `AuthIntegrationTest` |
| RF02 login/JWT | `PatientJourney` (passo 2), `BusinessRules` RN06/RN07 |
| RF03 busca, filtro e ordenação | `SearchExamsIntegrationTest`, `BusinessRules` RN17/RN19/RN38 |
| RF04 média e economia | `SearchExamsIntegrationTest`, `BusinessRules` RN15 |
| RF05 detalhes da oferta | `ExamAndClinicDetailsIntegrationTest`, `PatientJourney` |
| RF06 perfil da clínica | `ExamAndClinicDetailsIntegrationTest`, `PatientJourney` |
| RF07 em alta | `ContactAndTrendingIntegrationTest`, `BusinessRules` RN30 |
| RF08 favoritos | `FavoritesIntegrationTest`, `PatientJourney`, `BusinessRules` RN35 |
| RF09 avaliações | `ReviewsIntegrationTest`, `PatientJourney` (inclui RN25 e RN27) |
| RF10 contato WhatsApp | `ContactAndTrendingIntegrationTest`, `PatientJourney`, RN28 |
| RF11 total economizado | `SavingsIntegrationTest`, `PatientJourney` |
| RF12 perfil, senha e foto | `ProfileIntegrationTest`, `PatientJourney` |
| RF13 histórico de preço | `SeedDataConsistency` (RN13), `DataModel` |
| RF14–RF17 (app) | Dispositivo: testados no app (`flutter test`) e manualmente |

### Requisitos não funcionais

| Requisito | Teste |
|---|---|
| RNF01 BCrypt | `AuthIntegrationTest` (hash `$2`), `BusinessRules` RN36 |
| RNF02 JWT stateless | `AuthIntegrationTest`, `BusinessRules` RN07 |
| RNF04 paginação | `BusinessRules` RN21 |
| RNF05 migrações versionadas | `DataModel` (Flyway contíguo e sem falha) |
| RNF06 OpenAPI | `OpenApiIntegrationTest`, `BusinessRules`, smoke |
| RNF07 RFC 9457 | `BusinessRules` (404, validação, malformado), smoke |
| RNF08 testabilidade | Toda a suíte (PostgreSQL real) |
| RNF09 LGPD / localização | `DataModel` + `BusinessRules` RN20 |
| RNF10 portabilidade | Build da imagem no CI e smoke pós-deploy |

### Regras de negócio

| Regra | Teste |
|---|---|
| RN01 telefone único | `PhoneTest`, `DataModel`, `AuthIntegrationTest` |
| RN03 senha forte | `PasswordPolicyTest`, `AuthIntegrationTest` |
| RN04 e-mail único | `DataModel`, `AuthIntegrationTest` |
| RN05 papel PATIENT | `BusinessRules`, `DataModel` |
| RN06 login sem enumeração | `BusinessRules` |
| RN07 token de 1 hora | `BusinessRules` |
| RN09–RN11 oferta, categoria, preço | `DataModel` |
| RN12 só ofertas disponíveis | `BusinessRules` |
| RN13 histórico de preço | `SeedDataConsistency`, `DataModel` |
| RN14/RN15 média, economia, melhor preço | `ExamOfferTest`, `SearchExams`, `BusinessRules` |
| RN16/RN39 busca sem acento e por apelido | `SearchExamsIntegrationTest` |
| RN17–RN19 ordenação e distância | `ExamSortAndCategoryTest`, `GeoPointTest`, `BusinessRules` |
| RN20 localização não persiste | `DataModel`, `BusinessRules` |
| RN21 paginação | `SearchExams`, `BusinessRules` |
| RN22 aberta agora | `ClinicOpeningTest`, `DataModel` (horários) |
| RN23 favorito idempotente | `FavoritesIntegrationTest`, `DataModel` |
| RN24 só autenticados | `BusinessRules` |
| RN25–RN27 avaliação | `ReviewTest`, `ReviewsIntegrationTest`, `DataModel`, `PatientJourney`, `SeedDataConsistency` |
| RN28 link do WhatsApp | `WhatsAppLinkBuilderTest`, `PatientJourney` |
| RN29–RN31 contato, em alta, economia | `ContactAndTrending`, `SavingsIntegrationTest`, `DataModel` |
| RN32–RN34 perfil e foto | `UserProfileTest`, `AvatarImageTest`, `ProfileIntegrationTest` |
| RN35 isolamento por usuário | `BusinessRules` |
| RN36 sem dados sensíveis | `BusinessRules` |
| RN37 CPF | `CpfTest`, `AuthIntegrationTest`, `DataModel` |
| RN38 filtros e bairros | `SearchExamsIntegrationTest`, `BusinessRules` |

## Limitação conhecida

A busca por apelido soma termos: "ecografia da barriga" casa com "ecografia" (genérico, três tipos de ultrassom) e com "barriga" (específico). O resultado inclui mais exames do que o ideal. Uma melhoria futura é pontuar a especificidade e ficar só com o melhor casamento.

## Como rodar

```bash
./mvnw test                                          # tudo (precisa de Docker)
./mvnw test -Dtest='BusinessRulesIntegrationTest'    # uma classe
deploy/smoke-test.sh http://localhost:8080           # smoke contra uma API em execução
```

No GitHub Actions, o relatório fica em **Actions → execução → Artifacts → relatorios-de-teste** e o resumo na própria página da execução.
