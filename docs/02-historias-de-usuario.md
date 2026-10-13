# 02 · Histórias de Usuário

Formato: **Como** `<ator>`, **quero** `<ação>`, **para** `<benefício>`. Critérios de aceite em Gherkin. Cada história cita os requisitos (`RFxx`) e o caso de uso (`UCxx`) que ela cobre.

## Épico A · Conta e acesso

### HU01 · Criar minha conta
Como **visitante**, quero me cadastrar com nome, telefone e senha, para favoritar exames e avaliar clínicas. *(RF01 · UC01)*

```gherkin
Cenário: Cadastro válido
  Dado que o telefone "(81) 99876-5432" não está cadastrado
  Quando eu envio nome, telefone e uma senha com 8 caracteres ou mais
  Então a conta é criada com o papel PATIENT
  E eu recebo um token de acesso

Cenário: Telefone já cadastrado
  Dado que o telefone "(81) 99876-5432" já está cadastrado
  Quando eu tento me cadastrar com ele
  Então recebo erro 409 informando que o telefone já está em uso

Cenário: Senha fraca
  Quando eu envio uma senha com menos de 8 caracteres
  Então recebo erro 400 apontando o campo "password"
```

### HU02 · Entrar no app
Como **paciente**, quero entrar com telefone e senha, para acessar minhas informações. *(RF02 · UC02)*

```gherkin
Cenário: Login válido
  Dado que tenho uma conta
  Quando informo telefone e senha corretos
  Então recebo um token JWT com user_id, role e exp

Cenário: Credenciais inválidas
  Quando informo telefone ou senha incorretos
  Então recebo erro 401 com mensagem genérica
  E a resposta não revela qual dos dois estava errado
```

## Épico B · Descobrir e comparar preços

### HU03 · Buscar exames
Como **visitante**, quero buscar um exame por nome e filtrar por categoria, para achar o que preciso rapidamente. *(RF03 · UC03)*

```gherkin
Cenário: Busca por texto
  Quando busco "ultrassom"
  Então vejo as ofertas cujo exame contém "ultrassom", ignorando maiúsculas e acentos

Cenário: Filtro por categoria
  Quando filtro pela categoria "Sangue"
  Então vejo só ofertas dessa categoria

Cenário: Nada encontrado
  Quando busco um termo sem correspondência
  Então recebo lista vazia com total 0
```

### HU04 · Comparar preços entre clínicas
Como **visitante**, quero ver as clínicas que oferecem o mesmo exame ordenadas por preço, distância ou nota, para escolher a melhor opção. *(RF03, RF04, RF14 · UC03)*

```gherkin
Cenário: Menor preço primeiro
  Dado que 4 clínicas oferecem "Ultrassom Abdominal Total"
  Quando ordeno por preço
  Então a oferta mais barata aparece primeiro com o selo "Melhor preço"

Cenário: Economia sobre a média
  Quando a lista é exibida
  Então cada oferta informa a diferença para o preço médio local

Cenário: Ordenar por distância
  Dado que informo minha latitude e longitude
  Quando ordeno por distância
  Então cada oferta traz a distância em km e a lista vem do mais perto para o mais longe

Cenário: Ordenar por distância sem localização
  Quando ordeno por distância sem informar latitude e longitude
  Então recebo erro 400
```

### HU05 · Ver detalhes do exame
Como **visitante**, quero ver o preparo, as formas de pagamento e a clínica, para saber se consigo fazer o exame. *(RF05 · UC04)*

```gherkin
Cenário: Detalhes completos
  Quando abro uma oferta existente
  Então vejo título, categoria, preparo, preço, formas de pagamento e dados da clínica
  E vejo se a clínica está aberta agora

Cenário: Oferta inexistente ou indisponível
  Quando abro uma oferta que não existe
  Então recebo erro 404
```

### HU06 · Conhecer a clínica
Como **visitante**, quero ver o perfil da clínica, para confiar nela antes de ir. *(RF06 · UC05)*

```gherkin
Cenário: Perfil público
  Quando abro uma clínica
  Então vejo nome, endereço, horários, nota média, número de avaliações e quantidade de exames
```

### HU07 · Ver o que está em alta
Como **visitante**, quero ver os exames mais procurados da semana, para ter referência do que as pessoas buscam. *(RF07 · UC06)*

```gherkin
Cenário: Ranking semanal
  Quando abro a tela inicial
  Então vejo até 5 exames ordenados pelo número de contatos nos últimos 7 dias
  E cada um mostra o menor preço ("a partir de")
```

## Épico C · Contato e economia

### HU08 · Chamar a clínica no WhatsApp
Como **visitante**, quero falar com a clínica pelo WhatsApp com a mensagem já pronta, para agendar sem digitar. *(RF10 · UC07)*

```gherkin
Cenário: Contato registrado
  Quando toco em "Chamar no WhatsApp" em uma oferta
  Então o sistema registra o contato
  E devolve o link "https://wa.me/<número>?text=<mensagem codificada>"

Cenário: Economia computada para paciente logado
  Dado que estou autenticado e o preço da oferta está abaixo da média local
  Quando registro o contato
  Então a economia estimada é a diferença entre a média e o preço
```

### HU09 · Ver quanto economizei
Como **paciente**, quero ver o total economizado, para perceber o valor do app. *(RF11 · UC08)*

```gherkin
Cenário: Total acumulado
  Quando abro meu perfil
  Então vejo a soma das economias dos meus contatos, nunca negativa
```

## Épico D · Favoritos e avaliações

### HU10 · Salvar exames favoritos
Como **paciente**, quero favoritar exames, para acompanhar o preço e achar rápido depois. *(RF08 · UC09)*

```gherkin
Cenário: Favoritar
  Quando favorito um exame
  Então ele aparece na minha lista de favoritos com o menor preço atual

Cenário: Favoritar duas vezes
  Dado que já favoritei o exame
  Quando favorito de novo
  Então nada muda e a resposta é de sucesso (idempotente)

Cenário: Sem login
  Quando tento favoritar sem estar autenticado
  Então recebo erro 401
```

### HU11 · Avaliar uma clínica
Como **paciente**, quero avaliar uma clínica com nota e comentário, para ajudar outros pacientes. *(RF09 · UC10)*

```gherkin
Cenário: Primeira avaliação
  Quando envio nota 5 e um comentário
  Então a avaliação é salva
  E a nota média da clínica é recalculada

Cenário: Avaliar de novo
  Dado que já avaliei a clínica
  Quando envio outra avaliação
  Então a anterior é substituída (uma avaliação por paciente e clínica)

Cenário: Nota inválida
  Quando envio nota 0 ou 6
  Então recebo erro 400
```

## Épico E · Perfil

### HU12 · Gerenciar meu perfil
Como **paciente**, quero editar meus dados e minha foto, para manter meu cadastro correto. *(RF12 · UC11)*

```gherkin
Cenário: Editar dados
  Quando altero nome, e-mail e data de nascimento
  Então os dados são atualizados

Cenário: Telefone não editável por aqui
  Quando tento alterar o telefone pelo perfil
  Então o campo é ignorado (o telefone é o identificador de login)

Cenário: Atualizar foto
  Quando envio uma imagem JPEG ou PNG de até 2 MB
  Então a foto é salva e a URL aparece no meu perfil
```

### HU13 · Trocar a senha
Como **paciente**, quero alterar minha senha, para manter minha conta segura. *(RF12 · UC12)*

```gherkin
Cenário: Senha atual correta
  Quando informo a senha atual correta e uma nova senha válida
  Então a senha é trocada

Cenário: Senha atual incorreta
  Quando informo a senha atual errada
  Então recebo erro 400 e a senha não muda
```

## Histórias de dispositivo (app)

| ID | História | Recurso | Req. |
|---|---|---|---|
| HU14 | Como paciente, quero ver a distância até cada clínica, para escolher a mais perto. | GPS (`geolocator`) | RF14 |
| HU15 | Como paciente, quero buscar falando, para não digitar. | Voz (`speech_to_text`) | RF15 |
| HU16 | Como paciente, quero tirar uma foto ou escolher da galeria para meu perfil. | Câmera (`image_picker`) | RF16 |
| HU17 | Como paciente, quero ser avisado quando o preço de um favorito cair e lembrado do jejum do exame. | Notificações locais | RF17 |

## Histórias reservadas para a Entrega 3

HU18 Recuperar senha · HU19 Ver e limpar histórico de buscas · HU20 Aceitar termos e exportar ou excluir meus dados · HU21 Manter a sessão com refresh token · HU22 Entrar com biometria · HU23 Compartilhar exame · HU24 Usar o app offline.
