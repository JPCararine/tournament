# Tech-Spec: Inscrição de Equipes — Campeonato CS2

> Documento gerado seguindo o modelo de Engenharia de Contexto.
> Cada tag XML abaixo pode ser copiada como prompt para uma IA executora.

---

## `<role>`

```xml
<role>
Você é um Engenheiro Backend sênior especialista em Spring Boot 3/4 (Java 21),
Spring Data JPA, integração com gateways de pagamento Pix (AbacatePay) via webhook,
e envio transacional de e-mail (SendGrid). Você prioriza simplicidade: o sistema
NÃO tem login nem painel administrativo — é uma aplicação pública enxuta.
</role>
```

## `<context>`

```xml
<context>
Projeto: aplicação de inscrição para um Campeonato de Counter-Strike 2 (CS2).
Repositório atual: esqueleto Spring Boot já criado em
C:\Users\joaop\OneDrive\Desktop\project

Estado atual do repositório (pom.xml + src):
- Spring Boot 4.1.0, Java 21.
- groupId: com.cs2event.project  | artifactId: project
- Package base: com.cs2event.project (única classe: ProjectApplication.java)
- Dependências já presentes: spring-boot-starter-webmvc, spring-boot-starter-thymeleaf,
  spring-boot-devtools, lombok (+ starters de teste).
- AINDA NÃO EXISTEM: spring-data-jpa, driver postgresql, validation,
  cliente HTTP para gateway, biblioteca de e-mail. Precisam ser adicionados ao pom.xml.
- src/main/resources tem pastas static/ e templates/ (Thymeleaf disponível, mas o
  front-end será entregue separadamente pelo time de front e consumirá a API por JSON).

Modelo de negócio (extraído do PDF oficial "Campeonato_CS2_Formulario_Atualizado.pdf"):
- O formulário é uma "Manifestação de Interesse". Preencher NÃO garante vaga.
- A vaga só é CONFIRMADA após a confirmação do pagamento da taxa de inscrição.
- A ordem de confirmação segue a ordem dos pagamentos recebidos.
- A quantidade de equipes pendentes (manifestaram interesse) e confirmadas (pagaram)
  deve ser exibida publicamente — é o "dashboard" pedido.

Jornada desejada:
1) Capitão preenche o formulário no front e envia.
2) Backend grava a equipe como PENDENTE e cria uma cobrança Pix no AbacatePay.
3) Backend envia um e-mail (SendGrid) ao e-mail do capitão contendo:
   - o link de convite do Discord da organização;
   - o QR-code Pix + o código copia-e-cola (e/ou link da cobrança AbacatePay).
4) Capitão paga o Pix.
5) AbacatePay chama o webhook do backend → equipe passa para CONFIRMADA no dashboard.

Decisões já tomadas (NÃO revisar):
- Stack backend: Spring Boot existente (Java 21).
- Gateway de pagamento: AbacatePay (Pix dinâmico com QR-code + copia-e-cola + webhook).
- E-mail transacional: SendGrid.
- Persistência: PostgreSQL gerenciado (Supabase ou Neon).
- Sem login, sem cadastro de usuário, sem área administrativa.
</context>
```

## `<objective>`

```xml
<objective>
Implementar, dentro do projeto Spring Boot com.cs2event.project, uma API pública sem
login que: (a) receba a inscrição de uma equipe via formulário, (b) gere uma cobrança
Pix no AbacatePay, (c) dispare um e-mail via SendGrid com link do Discord + QR-code/código
Pix, (d) receba o webhook de pagamento do AbacatePay e marque a equipe como CONFIRMADA,
e (e) exponha um endpoint de dashboard com a contagem/lista de equipes PENDENTES e
CONFIRMADAS.
</objective>
```

## `<constraints>`

```xml
<constraints>
- Manter o package base com.cs2event.project e o padrão de projeto Spring Boot existente.
- Java 21 + Lombok já em uso — usar records para DTOs e Lombok para entidades.
- Sem autenticação de usuário. PORÉM o endpoint de webhook DEVE validar a autenticidade
  da chamada do AbacatePay (secret/assinatura) — não confiar em request anônimo.
- O status CONFIRMADA NUNCA é definido pelo front nem pelo retorno da criação da cobrança.
  Só o webhook de pagamento confirmado pode promover PENDENTE -> CONFIRMADA (server-side).
- O webhook precisa ser IDEMPOTENTE: o AbacatePay pode reenviar o mesmo evento; processar
  duas vezes não pode duplicar confirmação nem reenviar e-mail.
- O checkbox "concordo com os termos" é obrigatório: rejeitar inscrição com termsAccepted=false
  (validação @AssertTrue), retornando 400.
- Validar e-mail, campos obrigatórios e que availability não seja vazia.
- Segredos (API key SendGrid, token AbacatePay, secret do webhook, URL do banco) ficam em
  variáveis de ambiente / application.properties, nunca hardcoded.
- A criação da cobrança e o envio de e-mail não podem derrubar a inscrição: se o e-mail
  falhar, a equipe já está gravada como PENDENTE e o erro é logado (não perder o registro).
- CORS habilitado para o domínio do front (configurável por env).
</constraints>
```

## `<plan_mode>`

```xml
<plan_mode>
1) Atualizar pom.xml: adicionar spring-boot-starter-data-jpa, spring-boot-starter-validation,
   org.postgresql:postgresql, e cliente de e-mail SendGrid (com.sendgrid:sendgrid-java) ou
   chamada HTTP direta à API SendGrid via RestClient.
2) Configurar application.properties: datasource Postgres (Supabase/Neon), JPA ddl-auto,
   e propriedades custom: abacatepay.token, abacatepay.webhook-secret, sendgrid.api-key,
   app.discord-invite-url, app.cors.allowed-origins.
3) Modelar a entidade Team + enum TeamStatus + repositório.
4) Camada de serviço:
   - TeamService.register(): persiste PENDENTE -> chama PaymentService -> chama EmailService.
   - PaymentService: integra AbacatePay (cria cobrança Pix, retorna brCode/QR/link/billingId).
   - EmailService: monta e envia o e-mail via SendGrid.
5) Controllers REST:
   - TeamController: POST /api/teams (inscrição) + GET /api/teams (dashboard).
   - PaymentWebhookController: POST /api/webhooks/abacatepay (confirmação).
6) Implementar idempotência + validação de secret no webhook.
7) Definir o mapeamento availability (List<String>) — @ElementCollection ou coluna jsonb.
8) Testes: registro válido/ inválido, webhook confirma equipe, webhook duplicado é no-op.
</plan_mode>
```

## `<entities>`

```xml
<entities>
Team (tabela: team) — agregado raiz da inscrição:
- id            UUID (PK, gerado)
- teamName      String        — "Nome do time"
- captainName   String        — "Capitão / responsável"
- captainEmail  String        — e-mail do capitão (destino do e-mail)
- captainDiscordId String     — Discord do capitão (ex.: "fulano#0001" ou id)
- whatsapp      String        — WhatsApp para contato
- availability  List<String>  — disponibilidade. Ex.: ["Segunda - Manhã","Terça - Tarde"]
                                (valores derivados da grade 7 dias x 3 turnos do PDF)
- observations  String (nullable) — campo "Observações" do PDF (opcional)
- termsAccepted boolean       — checkbox "concordo com os termos" (deve ser true)
- status        TeamStatus    — PENDENTE | CONFIRMADA (default PENDENTE)
- billingId     String        — id da cobrança no AbacatePay (chave p/ casar o webhook)
- amountCents   Integer       — valor da taxa de inscrição em centavos
- createdAt     Instant       — momento da manifestação de interesse
- confirmedAt   Instant (nullable) — momento em que o webhook confirmou o pagamento

TeamStatus (enum): PENDENTE, CONFIRMADA

Mapeamento de availability: usar @ElementCollection (tabela team_availability) OU
coluna jsonb (org.hibernate.type / @JdbcTypeCode(SqlTypes.JSON)). Preferir jsonb no
Postgres para evitar tabela auxiliar, dado que é só leitura/escrita em bloco.
</entities>
```

## `<flows>`

```xml
<flows>
Fluxo A — Inscrição (POST /api/teams):
1) Front envia JSON: { teamName, captainName, captainEmail, captainDiscordId, whatsapp,
   availability[], observations?, termsAccepted }.
2) Bean Validation: campos obrigatórios + termsAccepted == true + availability não vazia.
3) TeamService grava Team com status=PENDENTE, createdAt=now.
4) PaymentService.createPixCharge(team): chama AbacatePay, recebe { billingId, brCode
   (copia-e-cola), brCodeBase64 (QR-code), url }. Persiste billingId/amount na Team.
5) EmailService.sendInviteAndCharge(team, charge): SendGrid envia ao captainEmail um
   e-mail com: link do Discord (app.discord-invite-url) + QR-code (imagem base64) +
   código copia-e-cola + link da cobrança. Falha de e-mail é logada, não aborta.
6) Resposta 201 com { id, status: "PENDENTE" } (sem expor dados sensíveis).

Fluxo B — Confirmação de pagamento (POST /api/webhooks/abacatepay):
1) AbacatePay chama o webhook quando o Pix é pago (evento de pagamento confirmado).
2) Controller valida o secret/assinatura (query param webhookSecret ou header) — se
   inválido, responde 401 e ignora.
3) Extrai o billingId do payload, busca a Team por billingId.
4) Se Team já está CONFIRMADA -> responde 200 e não faz nada (idempotência).
5) Senão -> status=CONFIRMADA, confirmedAt=now, salva. Responde 200.

Fluxo C — Dashboard (GET /api/teams):
1) Retorna { pendentes: [...], confirmadas: [...], totalPendentes, totalConfirmadas }.
2) Cada item expõe apenas dados públicos (ex.: teamName, status). NÃO expor e-mail,
   whatsapp, discord nem billingId no payload público do dashboard.
</flows>
```

## `<technical_context>`

```xml
<technical_context>
Stack confirmada:
- Spring Boot 4.1.0, Java 21, Maven (mvnw já no repo). Lombok ativo.
- Spring Web MVC (REST). Thymeleaf presente mas o front é entregue à parte (API JSON).
- Spring Data JPA + Hibernate -> PostgreSQL (Supabase/Neon, via DATABASE_URL).
- HTTP client para AbacatePay: usar org.springframework.web.client.RestClient (nativo
  do Spring 6/Boot 3+), com base URL https://api.abacatepay.com e Bearer token.
- E-mail: SendGrid (com.sendgrid:sendgrid-java OU POST direto a
  https://api.sendgrid.com/v3/mail/send via RestClient com Bearer API key).

Estrutura de pacotes proposta (ONDE CADA COISA FICA NO BACKEND):
com.cs2event.project
├── ProjectApplication.java            (já existe)
├── config/
│   ├── CorsConfig.java                (libera origem do front)
│   └── HttpClientConfig.java          (beans RestClient p/ AbacatePay e SendGrid)
├── team/
│   ├── Team.java                      (@Entity)
│   ├── TeamStatus.java               (enum PENDENTE/CONFIRMADA)
│   ├── TeamRepository.java           (extends JpaRepository, findByBillingId)
│   ├── TeamService.java              (orquestra registro -> cobrança -> e-mail)
│   ├── TeamController.java           (POST /api/teams, GET /api/teams)
│   └── dto/
│       ├── TeamRegistrationRequest.java   (record + Bean Validation)
│       └── DashboardResponse.java         (record público)
├── payment/
│   ├── AbacatePayClient.java         (chamadas HTTP ao AbacatePay)
│   ├── PaymentService.java           (createPixCharge)
│   ├── PaymentWebhookController.java (POST /api/webhooks/abacatepay)
│   └── dto/                           (PixChargeRequest/Response, WebhookPayload)
└── email/
    └── EmailService.java             (SendGrid: sendInviteAndCharge)

application.properties (chaves a adicionar):
  spring.datasource.url / username / password   (Supabase/Neon)
  spring.jpa.hibernate.ddl-auto=update
  abacatepay.api-url, abacatepay.token, abacatepay.webhook-secret, abacatepay.amount-cents
  sendgrid.api-key, sendgrid.from-email
  app.discord-invite-url
  app.cors.allowed-origins

Observação sobre AbacatePay: confirmar na documentação oficial o endpoint exato de
criação de cobrança Pix (ex.: POST /v1/pixQrCode/create ou /v1/billing/create), o nome
do evento de webhook de pagamento confirmado e o mecanismo de assinatura/secret. Tratar
esses nomes como [confirmar na doc AbacatePay] e centralizar em AbacatePayClient.
</technical_context>
```

## `<anti_patterns>`

```xml
<anti_patterns>
- NÃO marcar equipe como CONFIRMADA no fluxo de criação da cobrança nem com base em
  retorno do front — só o webhook confirma.
- NÃO confiar no webhook sem validar o secret/assinatura (qualquer um poderia "confirmar"
  uma equipe de graça).
- NÃO deixar o webhook não-idempotente — reprocessar o mesmo evento não pode reconfirmar
  nem reenviar e-mail.
- NÃO vazar dados pessoais (e-mail, whatsapp, discord, billingId) no GET /api/teams público.
- NÃO hardcodar token AbacatePay nem API key SendGrid no código — usar env/properties.
- NÃO acoplar a regra de e-mail ao gateway: EmailService recebe os dados da cobrança já
  prontos, não conhece o AbacatePay.
- NÃO bloquear a resposta da inscrição por causa de e-mail lento/fora do ar — a equipe
  precisa ser persistida como PENDENTE mesmo se o e-mail falhar (logar e seguir).
- NÃO usar Thymeleaf para "esconder" lógica de negócio — o front é cliente externo da API.
</anti_patterns>
```

## `<deliverable>`

```xml
<deliverable>
Código Java pronto para compilar dentro de com.cs2event.project, organizado exatamente
na estrutura de pacotes descrita em <technical_context>, com:
- pom.xml atualizado (jpa, validation, postgresql, sendgrid/RestClient);
- entidade Team + enum + repositório;
- TeamController (POST /api/teams + GET /api/teams) e PaymentWebhookController;
- PaymentService/AbacatePayClient e EmailService com pontos de integração isolados
  e os nomes de endpoint do AbacatePay marcados como [confirmar na doc];
- application.properties com as chaves de configuração (valores por env, sem segredos);
- testes básicos (registro válido/inválido, webhook confirma, webhook duplicado no-op).
Acompanhar de um README curto explicando as variáveis de ambiente necessárias
(DATABASE_URL, ABACATEPAY_TOKEN, ABACATEPAY_WEBHOOK_SECRET, SENDGRID_API_KEY,
SENDGRID_FROM_EMAIL, DISCORD_INVITE_URL) e o contrato JSON de POST /api/teams.
</deliverable>
```
