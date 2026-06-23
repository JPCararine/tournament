# Inscrição de Equipes — Campeonato CS2

API pública (sem login) para manifestação de interesse e confirmação de equipes no
Campeonato de Counter-Strike 2. Preencher o formulário **não garante vaga**: a vaga só é
**CONFIRMADA** após o pagamento da taxa de inscrição (Pix via Asaas), na ordem em que
os pagamentos são recebidos.

## Jornada

1. Capitão envia o formulário → equipe gravada como **PENDENTE**.
2. Backend cria um QR Code Pix estatico no **Asaas**.
3. Backend envia e-mail (**SendGrid**) ao capitão com link do Discord + QR-code/copia-e-cola.
4. Capitão paga o Pix.
5. Asaas chama o webhook → equipe passa para **CONFIRMADA**.

## Stack

- Spring Boot 4.1 / Java 21, Spring Web MVC + Spring Data JPA (PostgreSQL).
- Cliente HTTP `RestClient` para o Asaas; `sendgrid-java` para e-mail.

## Variáveis de ambiente

| Variável | Obrigatória | Descrição |
|---|---|---|
| `DATABASE_URL` | sim | JDBC do Postgres, ex.: `jdbc:postgresql://host:5432/db` (Supabase/Neon) |
| `DATABASE_USERNAME` | sim* | Usuário do banco (se não embutido na URL) |
| `DATABASE_PASSWORD` | sim* | Senha do banco (se não embutida na URL) |
| `ASAAS_API_URL` | não | Base URL da API (default `https://api-sandbox.asaas.com`) |
| `ASAAS_API_KEY` | sim | API key enviada no header `access_token` |
| `ASAAS_WEBHOOK_TOKEN` | sim | Token usado para validar o header `asaas-access-token` do webhook |
| `ASAAS_PIX_KEY` | sim | Chave Pix que recebera os pagamentos do QR Code |
| `ASAAS_AMOUNT_CENTS` | não | Taxa de inscrição em centavos (default `5000` = R$ 50,00) |
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

### POST `/webhooks/asaas` — confirmação de pagamento

- Autenticidade obrigatória: token no header `asaas-access-token`.
  Token inválido/ausente → `401`.
- Idempotente: reenvio do mesmo evento não reconfirma nem reenvia e-mail.
- Apenas este endpoint promove `PENDENTE → CONFIRMADA` (nunca o front).

## Notas de integração (Asaas)

A criacao do QR Code estatico esta centralizada em `payment/AsaasClient.java`, usando
`POST /v3/pix/qrCodes/static` com `addressKey`, `value`, `format=ALL`,
`expirationSeconds`, `allowsMultiplePayments=false` e `externalReference` com o UUID da
equipe. O webhook aceita `PAYMENT_RECEIVED` e `PAYMENT_CONFIRMED`; para QR Code estatico,
o id salvo em `billingId` vem de `payment.pixQrCodeId`.

## Persistência de `availability`

Mapeada como `@ElementCollection` (tabela `team_availability`) para portabilidade entre
Postgres e o H2 dos testes. Alternativa no Postgres: coluna `jsonb` via
`@JdbcTypeCode(SqlTypes.JSON)` — troca pontual na entidade `Team`.
