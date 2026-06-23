# Frontend Integration - Fluxo Atual

Este documento descreve o que o frontend precisa fazer depois da mudanca do pagamento Pix.

## Resumo Do Fluxo

1. O usuario preenche o formulario no frontend.
2. O frontend chama `POST /api/teams`.
3. O backend cria a cobranca Pix no Asaas, salva QR Code/copia-e-cola e envia um e-mail ao capitao.
4. O e-mail contem apenas um link de pagamento.
5. Em producao, esse link deve apontar para a rota de pagamento do frontend.
6. A rota do frontend busca os dados no backend em `GET /api/payments/{billingId}` com `Accept: application/json`.
7. O backend retorna:
   - status do pagamento;
   - codigo Pix copia-e-cola;
   - link do Discord.
8. A imagem do QR Code vem de `GET /api/payments/{billingId}/qr.png`, somente enquanto o pagamento estiver pendente.
9. O status do time muda para `CONFIRMADA` apenas quando o webhook do Asaas confirma o pagamento.

## Variavel Do Front

Configure a base URL da API:

```env
VITE_API_URL=https://api.seu-dominio.com
```

Em desenvolvimento:

```env
VITE_API_URL=http://localhost:8080
```

No backend, configure a base da rota de pagamento do frontend:

```env
PAYMENT_PAGE_BASE_URL=https://site.seu-dominio.com/pagamento
```

O e-mail vai montar o link assim:

```txt
{PAYMENT_PAGE_BASE_URL}/{billingId}
```

## Endpoint De Inscricao

```http
POST /api/teams
Content-Type: application/json
```

### Request

```ts
type TeamRegistrationRequest = {
  teamName: string;
  captainName: string;
  captainEmail: string;
  captainDiscordId: string;
  whatsapp: string;
  availability: string[];
  observations?: string | null;
  termsAccepted: boolean;
};
```

Exemplo:

```json
{
  "teamName": "SG TEAM",
  "captainName": "Joao Pedro",
  "captainEmail": "joao@example.com",
  "captainDiscordId": "joaopedro",
  "whatsapp": "21999999999",
  "availability": ["Sabado - Tarde", "Domingo - Noite"],
  "observations": null,
  "termsAccepted": true
}
```

### Response `201`

```json
{
  "id": "d8fe7943-eb7f-440b-a422-5d64d21fe503",
  "status": "PENDENTE"
}
```

Importante: essa resposta nao retorna QR Code, copia-e-cola, `billingId` ou link de pagamento. O pagamento vai por e-mail para o capitao.

### UX Apos Sucesso

Ao receber `201`, mostrar uma tela/mensagem assim:

```txt
Inscricao recebida!
Enviamos o link de pagamento Pix para o e-mail do capitao.
A vaga sera confirmada automaticamente apos o pagamento.
```

Nao tente redirecionar para uma pagina de pagamento no frontend, porque o link publico e gerado pelo backend e enviado por e-mail.

## Tratamento De Erros

### `400 Bad Request` - validacao

```json
{
  "message": "Dados de inscricao invalidos",
  "errors": {
    "captainEmail": "deve ser um endereco de e-mail bem formado",
    "termsAccepted": "E necessario concordar com os termos"
  }
}
```

Acao no front: exibir `errors[campo]` abaixo do input correspondente.

### `409 Conflict` - duplicidade ou inscricoes fechadas

Duplicidade:

```json
{
  "message": "Ja existe uma equipe inscrita com este nome."
}
```

Inscricoes fechadas:

```json
{
  "message": "O numero de inscritos chegou ao seu limite, agradecemos o seu interesse!",
  "registrationOpen": false
}
```

Acao no front: exibir a mensagem em um alerta/banner do formulario. Se `registrationOpen === false`, bloquear o envio do formulario.

### `502 Bad Gateway` - falha ao gerar Pix

```json
{
  "message": "Nao foi possivel gerar o Pix no momento. Tente novamente em alguns minutos."
}
```

Acao no front: exibir erro temporario e permitir tentar novamente.

## Exemplo De Cliente

```ts
const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function registerTeam(payload: TeamRegistrationRequest) {
  const response = await fetch(`${API_URL}/api/teams`, {
    method: "POST",
    headers: {
      "Content-Type": "application/json",
    },
    body: JSON.stringify(payload),
  });

  const data = await response.json().catch(() => ({}));

  if (response.status === 201) {
    return data as { id: string; status: "PENDENTE" };
  }

  if (response.status === 400) {
    throw { type: "validation", ...data };
  }

  if (response.status === 409) {
    throw { type: "conflict", ...data };
  }

  if (response.status === 502) {
    throw { type: "payment_gateway", ...data };
  }

  throw {
    type: "unknown",
    message: data.message ?? "Nao foi possivel concluir a inscricao.",
  };
}
```

## Endpoint Do Dashboard

```http
GET /api/teams
```

### Response `200`

```json
{
  "pendentes": [
    { "teamName": "SG TEAM", "status": "PENDENTE" }
  ],
  "confirmadas": [
    { "teamName": "Time Confirmado", "status": "CONFIRMADA" }
  ],
  "totalPendentes": 1,
  "totalConfirmadas": 1,
  "maxTeams": 16,
  "registrationOpen": true
}
```

Tipos:

```ts
type TeamStatus = "PENDENTE" | "CONFIRMADA";

type TeamSummary = {
  teamName: string;
  status: TeamStatus;
};

type DashboardResponse = {
  pendentes: TeamSummary[];
  confirmadas: TeamSummary[];
  totalPendentes: number;
  totalConfirmadas: number;
  maxTeams: number;
  registrationOpen: boolean;
};
```

Uso recomendado:

- Mostrar vagas como `totalConfirmadas / maxTeams`.
- Mostrar pendentes separadas de confirmadas.
- Se `registrationOpen` for `false`, desabilitar ou esconder o formulario.
- Atualizar o dashboard periodicamente, por exemplo a cada 30 segundos.

## Pagina De Pagamento No Frontend

O e-mail deve apontar para uma rota do frontend, por exemplo:

```txt
https://site.seu-dominio.com/pagamento/{billingId}
```

Nessa tela, o frontend deve buscar os dados no backend:

```http
GET /api/payments/{billingId}
Accept: application/json
```

### Response `200` - pendente

```json
{
  "status": "PENDENTE",
  "copyPaste": "000201...",
  "discordUrl": "https://discord.gg/seu-convite",
  "teamName": "SG TEAM"
}
```

### Response `200` - confirmado

```json
{
  "status": "CONFIRMADA",
  "copyPaste": null,
  "discordUrl": "https://discord.gg/seu-convite",
  "teamName": "SG TEAM"
}
```

Quando `status === "CONFIRMADA"`, nao mostrar QR Code nem copia-e-cola. Mostre uma tela de pagamento ja confirmado e o botao do Discord.

### QR Code PNG

Use o endpoint abaixo para a imagem quando `status === "PENDENTE"`:

```http
GET /api/payments/{billingId}/qr.png
```

Se o time ja estiver `CONFIRMADA`, esse endpoint retorna `404` para evitar exibir QR Code pago.

### Exemplo De Loader Da Pagina

```ts
export async function carregarPagamento(billingId: string) {
  const response = await fetch(`${API_URL}/api/payments/${billingId}`, {
    headers: {
      Accept: "application/json",
    },
  });

  if (response.status === 404) {
    throw { type: "not_found", message: "Pagamento nao encontrado." };
  }

  if (!response.ok) {
    throw { type: "unknown", message: "Nao foi possivel carregar o pagamento." };
  }

  return response.json() as Promise<{
    status: "PENDENTE" | "CONFIRMADA";
    copyPaste: string | null;
    discordUrl: string;
    teamName: string;
  }>;
}
```

O frontend nao recebe `billingId` no `POST /api/teams`. Ele chega ao usuario pelo link do e-mail.

## Checklist Para O Front

- Usar `POST /api/teams` no formulario.
- Ao sucesso, mostrar mensagem para checar o e-mail.
- Nao esperar QR Code na resposta da inscricao.
- Criar rota publica no frontend para `/pagamento/:billingId` ou equivalente.
- Configurar `PAYMENT_PAGE_BASE_URL` no backend apontando para essa rota.
- Na rota de pagamento, buscar `GET /api/payments/{billingId}` com `Accept: application/json`.
- Usar `/api/payments/{billingId}/qr.png` como `src` da imagem somente se `status === "PENDENTE"`.
- Se `status === "CONFIRMADA"`, mostrar pagamento confirmado e nao renderizar QR Code.
- Nao chamar Asaas pelo frontend.
- Tratar `400`, `409` e `502` separadamente.
- Usar `GET /api/teams` para dashboard.
- Bloquear inscricao quando `registrationOpen` for `false`.
- Garantir que `VITE_API_URL` aponta para o backend correto.
