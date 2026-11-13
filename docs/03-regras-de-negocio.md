# 03 · Regras de Negócio

Cada regra tem um identificador estável (`RN`) para ser citado em código, testes e commits. A coluna *Onde* diz em qual camada a regra é garantida.

## Conta e autenticação

| ID | Regra | Onde |
|---|---|---|
| RN01 | O telefone é o identificador de login e é **único**. É normalizado para somente dígitos com DDI 55 (ex.: `(81) 99876-5432` → `5581998765432`) antes de validar e salvar. | Domínio + `UNIQUE` no banco |
| RN02 | O telefone deve ser um celular brasileiro válido: DDD de 2 dígitos e número iniciado em 9 com 9 dígitos. | Bean Validation |
| RN03 | A senha tem no mínimo 8 caracteres, com ao menos uma letra e um número, e é guardada com BCrypt (custo ≥ 10). | Domínio + `PasswordEncoder` |
| RN04 | O e-mail é opcional; se informado, é único sem diferenciar maiúsculas. | Índice único parcial |
| RN05 | O cadastro público cria **sempre** o papel `PATIENT`. Papéis privilegiados não podem ser escolhidos pelo cliente. | Aplicação |
| RN06 | Falha de login devolve a mesma mensagem para telefone inexistente e senha errada (evita enumerar usuários). | Aplicação |
| RN07 | O token JWT contém `sub` (id do usuário), `role` e `exp`. Validade de 1 hora na Entrega 2. | Infraestrutura |
| RN08 | A troca de senha exige a senha atual correta e a nova deve ser diferente da atual. | Aplicação |

## Catálogo, ofertas e preços

| ID | Regra | Onde |
|---|---|---|
| RN09 | O **tipo de exame** (catálogo) é único e comum a todas as clínicas. A **oferta** é o tipo de exame vendido por uma clínica, com preço próprio. Cada clínica tem no máximo uma oferta por tipo. | Modelo + `UNIQUE (clinic_id, exam_type_id)` |
| RN10 | A categoria é uma de: Sangue, Raio-X, Consultas, Cardio, Ultrassom. | `CHECK` + enum |
| RN11 | O preço é em reais, com 2 casas decimais, e nunca negativo. Valores monetários usam `BigDecimal`, nunca `double`. | Domínio + `CHECK` |
| RN12 | Só ofertas com `available = true` aparecem na busca. | Consulta |
| RN13 | Toda criação de oferta e toda mudança de preço grava uma linha em `price_history`. | Aplicação |
| RN14 | O **preço médio local** de um exame é a média aritmética dos preços das ofertas disponíveis daquele tipo. | Consulta |
| RN15 | A **economia** de uma oferta é `max(0, média − preço)`. O selo "Melhor preço" vai para a oferta de menor preço (em empate, para todas as empatadas). | Domínio |
| RN16 | A busca por texto ignora maiúsculas e acentos e procura no título do tipo de exame. | Consulta (`unaccent`) |

## Busca, ordenação e localização

| ID | Regra | Onde |
|---|---|---|
| RN17 | Ordenações aceitas: `price` (padrão), `rating`, `name` e `distance`. Empates desempatam por nome. | Aplicação |
| RN18 | A ordenação `distance` exige `lat` e `lng` válidos (−90..90 e −180..180). | Aplicação |
| RN19 | A distância é a de **círculo máximo** (fórmula de Haversine), em km com uma casa decimal. | Domínio |
| RN20 | A localização do paciente não é persistida. Ela só existe durante a requisição. | Aplicação (LGPD) |
| RN21 | Toda listagem é paginada: `size` padrão 20 e máximo 50. | Aplicação |
| RN22 | Uma clínica está **aberta agora** se o horário atual (fuso `America/Recife`) cai dentro de um intervalo do dia da semana corrente em `clinic_operating_hours`. | Domínio |

## Favoritos, avaliações e contatos

| ID | Regra | Onde |
|---|---|---|
| RN23 | O paciente favorita um **tipo de exame**, não uma oferta. O alerta de preço considera o menor preço entre as clínicas. A operação é idempotente. | Aplicação + PK composta |
| RN24 | Só usuários autenticados favoritam, avaliam ou veem a economia. | Segurança |
| RN25 | Cada paciente tem **uma** avaliação por clínica. Reavaliar substitui a anterior. | `UNIQUE (user_id, clinic_id)` |
| RN26 | A nota é um inteiro de 1 a 5. O comentário tem até 1000 caracteres. | Bean Validation + `CHECK` |
| RN27 | Ao salvar uma avaliação, `clinics.rating` (média com 1 casa) e `clinics.review_count` são recalculados na mesma transação. | Aplicação |
| RN28 | O link do WhatsApp é `https://wa.me/{número}?text={mensagem}`. O número vem só com dígitos e a mensagem é codificada em URL. Texto sugerido: `Olá! Vi o exame {título} por R$ {preço} no Clinics e gostaria de agendar.` | Domínio |
| RN29 | Cada toque em "Chamar no WhatsApp" grava um evento de contato com o preço do momento e a economia estimada. O usuário pode ser nulo (visitante). | Aplicação |
| RN30 | **Exames em alta** = tipos de exame com mais contatos nos últimos 7 dias, até 5 itens. Sem dados suficientes, completa com os de menor preço médio. | Consulta |
| RN31 | O **total economizado** do paciente é a soma de `estimated_savings` dos seus contatos. | Consulta |

## Perfil e privacidade

| ID | Regra | Onde |
|---|---|---|
| RN32 | O telefone não é editável pelo perfil. Nome, e-mail e data de nascimento são. | Aplicação |
| RN33 | A data de nascimento não pode estar no futuro. | Bean Validation |
| RN34 | A foto aceita JPEG ou PNG de até 2 MB. | Aplicação |
| RN35 | O usuário só acessa e altera os **próprios** dados. O id vem do token, nunca do corpo ou da URL (previne IDOR). | Segurança |
| RN36 | Dados sensíveis (hash de senha, token) nunca aparecem em respostas ou logs. | Aplicação |
| RN37 | O cadastro exige CPF, e-mail e data de nascimento. O CPF é normalizado para 11 dígitos, tem os dígitos verificadores validados, é **único** e não é editável pelo perfil. Aparece formatado só no `GET /me`. | Domínio + índice único parcial |
| RN38 | A busca aceita filtros combináveis: bairro, faixa de preço, nota mínima da clínica e somente melhor preço. Média e menor preço continuam calculados sobre todas as ofertas do tipo (RN14). A lista de bairros vem das clínicas cadastradas. | Aplicação |
| RN39 | A busca entende nomes populares: cada tipo de exame tem apelidos (`exam_type_aliases`), como "colesterol" para Lipidograma ou "esteira" para Teste Ergométrico. O texto casa com o nome clínico ou com um apelido, ignorando caixa e acentos, e também quando o apelido aparece dentro de uma frase maior. | Consulta |

## Regras reservadas para a Entrega 3

- Recuperação de senha com código de 6 dígitos válido por 15 minutos e uso único.
- Histórico de buscas guarda no máximo as últimas 20 por paciente.
- A exclusão de conta anonimiza avaliações e contatos em vez de apagá-los.
- Rotas de gestão de exames exigem papel `CLINIC_MANAGER` e dono da clínica (`owner_user_id = usuário do token`), com resposta 403 em caso contrário.
