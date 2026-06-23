# Inscrição de Equipes — Campeonato CS2

API pública (sem login) para manifestação de interesse e confirmação de equipes no
Campeonato de Counter-Strike 2. Preencher o formulário **não garante vaga**: a vaga só é
**CONFIRMADA** após o pagamento da taxa de inscrição (Pix via AbacatePay), na ordem em que
os pagamentos são recebidos.

## Jornada

1. Capitão envia o formulário → equipe gravada como **PENDENTE**.
2. Backend cria uma cobrança Pix no **AbacatePay**.
3. Backend envia e-mail (**SendGrid**) ao capitão com link do Discord + QR-code/copia-e-cola.
4. Capitão paga o Pix.
5. AbacatePay chama o webhook → equipe passa para **CONFIRMADA**.

## Stack

- Spring Boot 4.1 / Java 21, Spring Web MVC + Spring Data JPA (PostgreSQL).
- Cliente HTTP `RestClient` para o AbacatePay; `sendgrid-java` para e-mail.

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DATABASE_URL` | sim | JDBC do Postgres, ex.: `jdbc:postgresql://host:5432/db` (Supabase/Neon) |
| `DATABASE_USERNAME` | sim* | Usuário do banco (se não embutido na URL) |
| `DATABASE_PASSWORD` | sim* | Senha do banco (se não embutida na URL) |
| `ABACATEPAY_API_URL` | não | Base URL da API (default `https://api.abacatepay.com`) |
| `ABACATEPAY_TOKEN` | sim | Bearer token do AbacatePay |
| `ABACATEPAY_WEBHOOK_SECRET` | sim | Secret usado para validar o webhook |
| `ABACATEPAY_AMOUNT_CENTS` | não | Taxa de inscrição em centavos (default `5000` = R$ 50,00) |
| `SENDGRID_API_KEY` | sim | API key do SendGrid |
| `SENDGRID_FROM_EMAIL` | sim | Remetente verificado no SendGrid |
| `DISCORD_INVITE_URL` | sim | Link de convite do Discord da organização |
| `CORS_ALLOWED_ORIGINS` | não | Origens do front separadas por vírgula (default `http://localhost:5173`) |

Nenhum segredo é hardcoded — tudo vem de env / `application.properties`.

## Como rodar

```bash
./mvnw spring-boot:run     # (mvnw.cmd no Windows)
./mvnw test                # testes (usam H2 em memória, não precisam de env)
```

## Contrato da API

### POST `/api/teams` — inscrição

Request:
```json
{
  "teamName": "Os Bravos",
  "captainName": "João Silva",
  "captainEmail": "joao@example.com",
  "captainDiscordId": "joao#0001",
  "whatsapp": "+5511999999999",
  "availability": ["Segunda - Manhã", "Terça - Tarde"],
  "observations": "opcional",
  "termsAccepted": true
}
```

- `availability` não pode ser vazia; `termsAccepted` deve ser `true`; `captainEmail` válido;
  demais campos (exceto `observations`) obrigatórios.

Resposta `201`:
```json
{ "id": "uuid", "status": "PENDENTE" }
```

Resposta `400` (validação):
```json
{ "message": "Dados de inscrição inválidos", "errors": { "termsAccepted": "É necessário concordar com os termos" } }
```

### GET `/api/teams` — dashboard público

Expõe apenas dados não sensíveis (nunca e-mail, whatsapp, discord ou billingId):
```json
{
  "pendentes": [{ "teamName": "Os Bravos", "status": "PENDENTE" }],
  "confirmadas": [{ "teamName": "Time X", "status": "CONFIRMADA" }],
  "totalPendentes": 1,
  "totalConfirmadas": 1
}
```

### POST `/api/webhooks/abacatepay` — confirmação de pagamento

- Autenticidade obrigatória: secret via query param `?webhookSecret=...` **ou** header
  `X-Webhook-Secret`. Secret inválido/ausente → `401`.
- Idempotente: reenvio do mesmo evento não reconfirma nem reenvia e-mail.
- Apenas este endpoint promove `PENDENTE → CONFIRMADA` (nunca o front).

## Notas de integração (AbacatePay)

Os nomes exatos de endpoint/campos do AbacatePay estão centralizados em
`payment/AbacatePayClient.java` e marcados com `[confirmar na doc AbacatePay]`. Caso a
documentação oficial divirja (endpoint de criação da cobrança, formato do payload do
webhook, nome do evento de pagamento confirmado), ajuste **apenas** essa classe — o
restante da aplicação fala em termos do record `PixCharge`.

## Persistência de `availability`

Mapeada como `@ElementCollection` (tabela `team_availability`) para portabilidade entre
Postgres e o H2 dos testes. Alternativa no Postgres: coluna `jsonb` via
`@JdbcTypeCode(SqlTypes.JSON)` — troca pontual na entidade `Team`.
