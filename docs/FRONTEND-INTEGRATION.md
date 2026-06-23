# Frontend Integration - Fluxo Atual

Este documento descreve o que o frontend precisa fazer depois da mudanca do pagamento Pix.

## Resumo Do Fluxo

1. O usuario preenche o formulario no frontend.
2. O frontend chama `POST /api/teams`.
3. O backend cria a cobranca Pix no Asaas, salva QR Code/copia-e-cola e envia um e-mail ao capitao.
4. O e-mail contem apenas um link de pagamento.
5. O link abre uma pagina do backend em `GET /api/payments/{billingId}` com:
   - imagem do QR Code Pix;
   - codigo Pix copia-e-cola;
   - botao do Discord.
6. O frontend nao precisa renderizar QR Code nem copia-e-cola.
7. O status do time muda para `CONFIRMADA` apenas quando o webhook do Asaas confirma o pagamento.

## Variavel Do Front

Configure a base URL da API:

```env
VITE_API_URL=https://api.seu-dominio.com
```

Em desenvolvimento:

```env
VITE_API_URL=http://localhost:8080
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

## Pagina De Pagamento

O frontend nao precisa consumir este endpoint diretamente no fluxo normal. Ele existe para o link enviado por e-mail:

```http
GET /api/payments/{billingId}
```

Essa pagina e HTML renderizado pelo backend e mostra:

- QR Code Pix;
- Pix copia-e-cola;
- link do Discord.

Tambem existe o PNG do QR Code:

```http
GET /api/payments/{billingId}/qr.png
```

O frontend nao recebe `billingId` no `POST /api/teams`, entao nao deve montar essa URL manualmente.

## Checklist Para O Front

- Usar `POST /api/teams` no formulario.
- Ao sucesso, mostrar mensagem para checar o e-mail.
- Nao esperar QR Code na resposta da inscricao.
- Nao chamar Asaas pelo frontend.
- Tratar `400`, `409` e `502` separadamente.
- Usar `GET /api/teams` para dashboard.
- Bloquear inscricao quando `registrationOpen` for `false`.
- Garantir que `VITE_API_URL` aponta para o backend correto.

