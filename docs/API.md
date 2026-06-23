# API do Backend — ESL ITAP (Inscrição de Campeonato CS2)

Documentação de todos os endpoints do backend e como consumi-los no frontend.

---

## Visão geral

O backend é uma **API REST** (Spring Boot). Ele **não** tem interface — o site/frontend
é uma aplicação separada (ex.: Vite/React em `http://localhost:5173`) que consome estes
endpoints.

| Item | Valor |
|------|-------|
| Base URL (dev) | `http://localhost:8080` |
| Formato | JSON (`Content-Type: application/json`) |
| Autenticação | Nenhuma nos endpoints públicos (`/api/**`). O webhook usa um secret. |
| CORS liberado para | `http://localhost:5173` (configurável via env `CORS_ALLOWED_ORIGINS`) |

### Fluxo completo

1. **Fluxo A** — usuário preenche o formulário no site → `POST /api/teams` → time gravado
   como `PENDENTE`, QR Code Pix estatico criado no Asaas e e-mail enviado ao capitão.
2. **Fluxo B** — capitão paga o Pix → Asaas chama `POST /webhooks/asaas` →
   time promovido para `CONFIRMADA`.
3. **Fluxo C** — o site exibe o painel público → `GET /api/teams` → listas de times
   pendentes e confirmados.

### Status de um time (`TeamStatus`)

| Valor | Significado |
|-------|-------------|
| `PENDENTE` | Formulário enviado, aguardando pagamento. |
| `CONFIRMADA` | Pagamento confirmado via webhook. **Só o webhook promove o status** — nunca o front. |

---

## 1. Inscrever equipe (Fluxo A)

Cria a inscrição, gera a cobrança Pix e dispara o e-mail de confirmação para o capitão.

```
POST /api/teams
Content-Type: application/json
```

### Corpo da requisição

| Campo | Tipo | Obrigatório | Regras |
|-------|------|-------------|--------|
| `teamName` | string | sim | não pode ser vazio |
| `captainName` | string | sim | não pode ser vazio |
| `captainEmail` | string | sim | e-mail válido |
| `captainDiscordId` | string | sim | não pode ser vazio |
| `whatsapp` | string | sim | não pode ser vazio |
| `availability` | string[] | sim | lista não vazia, sem itens em branco |
| `observations` | string | não | texto livre |
| `termsAccepted` | boolean | sim | precisa ser `true` |

```json
{
  "teamName": "Os Invencíveis",
  "captainName": "João Pedro",
  "captainEmail": "joao@example.com",
  "captainDiscordId": "joaopedro",
  "whatsapp": "11999998888",
  "availability": ["Segunda - Noite", "Sábado - Tarde"],
  "observations": "Preferência por jogos após as 20h",
  "termsAccepted": true
}
```

### Resposta `201 Created`

Não retorna dados sensíveis — apenas o id e o status.

```json
{
  "id": "d8fe7943-eb7f-440b-a422-5d64d21fe503",
  "status": "PENDENTE"
}
```

> O QR Code e o copia-e-cola **não** voltam nesta resposta — eles vão por e-mail ao capitão.

### Resposta `400 Bad Request` (validação)

```json
{
  "message": "Dados de inscrição inválidos",
  "errors": {
    "captainEmail": "deve ser um endereço de e-mail bem formado",
    "termsAccepted": "É necessário concordar com os termos",
    "availability": "não deve estar vazio"
  }
}
```

> O front deve mapear `errors[campo]` para a mensagem embaixo de cada input.

### Resposta `500 Internal Server Error`

Falha ao criar o QR Code Pix no Asaas (gateway fora do ar, token inválido, etc.).
A falha de **e-mail** não gera 500 — a inscrição é mantida como `PENDENTE` e o erro é
apenas logado.

### Exemplo — `fetch`

```js
async function inscreverEquipe(dados) {
  const resp = await fetch("http://localhost:8080/api/teams", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });

  if (resp.status === 201) {
    return await resp.json(); // { id, status }
  }
  if (resp.status === 400) {
    const { errors } = await resp.json();
    throw { tipo: "validacao", errors }; // exibir erros por campo
  }
  throw { tipo: "servidor", message: "Não foi possível concluir a inscrição. Tente novamente." };
}
```

### Exemplo — `curl`

```bash
curl -X POST http://localhost:8080/api/teams \
  -H "Content-Type: application/json" \
  -d '{
    "teamName":"Os Invencíveis","captainName":"João","captainEmail":"joao@example.com",
    "captainDiscordId":"joaopedro","whatsapp":"11999998888",
    "availability":["Sábado - Tarde"],"termsAccepted":true
  }'
```

---

## 2. Painel público / dashboard (Fluxo C)

Lista os times pendentes e confirmados. **Só dados não sensíveis** (nome + status).
Nunca expõe e-mail, whatsapp, discord ou id de cobrança.

```
GET /api/teams
```

### Resposta `200 OK`

```json
{
  "pendentes": [
    { "teamName": "Preview Team", "status": "PENDENTE" }
  ],
  "confirmadas": [
    { "teamName": "Os Invencíveis", "status": "CONFIRMADA" }
  ],
  "totalPendentes": 1,
  "totalConfirmadas": 1
}
```

> O limite do campeonato é **16 times confirmados**. Use `totalConfirmadas` para exibir
> as vagas (ex.: `1/16`).

### Exemplo — `fetch`

```js
async function carregarDashboard() {
  const resp = await fetch("http://localhost:8080/api/teams");
  return await resp.json(); // { pendentes, confirmadas, totalPendentes, totalConfirmadas }
}
```

---

## 3. Webhook de pagamento (Fluxo B) — uso interno do Asaas

> **Atenção:** este endpoint **não é chamado pelo frontend**. Quem chama é o Asaas,
> servidor-para-servidor, quando o pagamento é confirmado. Documentado aqui apenas para
> referência/configuração.

```
POST /webhooks/asaas
Content-Type: application/json
asaas-access-token: SEU_ASAAS_WEBHOOK_TOKEN
```

A autenticidade é validada pelo token do header `asaas-access-token`.
O endpoint é **idempotente**: reenvios do mesmo
pagamento não duplicam a confirmação.

### Configuração no painel do Asaas

URL a cadastrar (com o ngrok apontando para `localhost:8080`):

```
https://SEU-SUBDOMINIO.ngrok-free.dev/webhooks/asaas
```

### Corpo esperado

Para QR Code estatico, o Asaas envia o id do QR pago em `payment.pixQrCodeId`.
O `externalReference` recebe o UUID da equipe gravado na criacao do QR Code.

```json
{
  "event": "PAYMENT_RECEIVED",
  "payment": {
    "id": "pay_080225913252",
    "status": "RECEIVED",
    "pixQrCodeId": "9bea9bcd126b45c7939960f577be84d6",
    "externalReference": "d8fe7943-eb7f-440b-a422-5d64d21fe503"
  }
}
```

### Respostas

| Código | Quando |
|--------|--------|
| `200 OK` | Confirmado, ou já estava confirmado (idempotente), ou cobrança desconhecida (para o gateway parar de reenviar). |
| `400 Bad Request` | Payload vazio. |
| `401 Unauthorized` | Token ausente ou inválido. |

---

## Modelos de dados (referência)

```ts
// TypeScript para o frontend
type TeamStatus = "PENDENTE" | "CONFIRMADA";

interface TeamRegistrationRequest {
  teamName: string;
  captainName: string;
  captainEmail: string;
  captainDiscordId: string;
  whatsapp: string;
  availability: string[];
  observations?: string;
  termsAccepted: boolean;
}

interface TeamRegistrationResponse {
  id: string;       // UUID
  status: TeamStatus;
}

interface TeamSummary {
  teamName: string;
  status: TeamStatus;
}

interface DashboardResponse {
  pendentes: TeamSummary[];
  confirmadas: TeamSummary[];
  totalPendentes: number;
  totalConfirmadas: number;
}

interface ValidationError {
  message: string;
  errors: Record<string, string>; // campo -> mensagem
}
```

---

## Exemplo de integração no frontend (React)

### Cliente de API reutilizável

```js
// api.js
const BASE_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export async function inscreverEquipe(dados) {
  const resp = await fetch(`${BASE_URL}/api/teams`, {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify(dados),
  });
  if (resp.status === 201) return resp.json();
  if (resp.status === 400) throw { tipo: "validacao", ...(await resp.json()) };
  throw { tipo: "servidor" };
}

export async function carregarDashboard() {
  const resp = await fetch(`${BASE_URL}/api/teams`);
  if (!resp.ok) throw new Error("Falha ao carregar dashboard");
  return resp.json();
}
```

### Formulário de inscrição

```jsx
import { useState } from "react";
import { inscreverEquipe } from "./api";

export function FormularioInscricao() {
  const [erros, setErros] = useState({});
  const [enviando, setEnviando] = useState(false);
  const [ok, setOk] = useState(false);

  async function onSubmit(e) {
    e.preventDefault();
    setEnviando(true);
    setErros({});
    const form = new FormData(e.target);
    const dados = {
      teamName: form.get("teamName"),
      captainName: form.get("captainName"),
      captainEmail: form.get("captainEmail"),
      captainDiscordId: form.get("captainDiscordId"),
      whatsapp: form.get("whatsapp"),
      availability: form.getAll("availability"),
      observations: form.get("observations") || null,
      termsAccepted: form.get("termsAccepted") === "on",
    };
    try {
      await inscreverEquipe(dados);
      setOk(true); // mostrar "Cheque seu e-mail para pagar o Pix"
    } catch (err) {
      if (err.tipo === "validacao") setErros(err.errors);
      else alert("Erro ao inscrever. Tente novamente.");
    } finally {
      setEnviando(false);
    }
  }

  if (ok) return <p>Inscrição recebida! Enviamos o QR Code do Pix para o seu e-mail.</p>;

  return (
    <form onSubmit={onSubmit}>
      <input name="teamName" placeholder="Nome do time" />
      {erros.teamName && <small>{erros.teamName}</small>}

      <input name="captainName" placeholder="Nome do capitão" />
      {erros.captainName && <small>{erros.captainName}</small>}

      <input name="captainEmail" type="email" placeholder="E-mail" />
      {erros.captainEmail && <small>{erros.captainEmail}</small>}

      <input name="captainDiscordId" placeholder="Discord" />
      <input name="whatsapp" placeholder="WhatsApp" />

      {/* availability como checkboxes de mesmo name */}
      <label><input type="checkbox" name="availability" value="Segunda - Noite" /> Segunda - Noite</label>
      <label><input type="checkbox" name="availability" value="Sábado - Tarde" /> Sábado - Tarde</label>
      {erros.availability && <small>{erros.availability}</small>}

      <textarea name="observations" placeholder="Observações (opcional)" />

      <label><input type="checkbox" name="termsAccepted" /> Aceito os termos</label>
      {erros.termsAccepted && <small>{erros.termsAccepted}</small>}

      <button disabled={enviando}>{enviando ? "Enviando..." : "Inscrever"}</button>
    </form>
  );
}
```

### Dashboard com vagas

```jsx
import { useEffect, useState } from "react";
import { carregarDashboard } from "./api";

const LIMITE = 16;

export function Dashboard() {
  const [data, setData] = useState(null);

  useEffect(() => {
    carregarDashboard().then(setData);
    // opcional: atualizar a cada 30s
    const t = setInterval(() => carregarDashboard().then(setData), 30000);
    return () => clearInterval(t);
  }, []);

  if (!data) return <p>Carregando...</p>;

  return (
    <div>
      <h2>Vagas confirmadas: {data.totalConfirmadas}/{LIMITE}</h2>
      <section>
        <h3>Confirmadas</h3>
        <ul>{data.confirmadas.map((t) => <li key={t.teamName}>{t.teamName}</li>)}</ul>
      </section>
      <section>
        <h3>Pendentes</h3>
        <ul>{data.pendentes.map((t) => <li key={t.teamName}>{t.teamName}</li>)}</ul>
      </section>
    </div>
  );
}
```

---

## Resumo dos endpoints

| Método | Rota | Quem chama | Descrição |
|--------|------|-----------|-----------|
| `POST` | `/api/teams` | Frontend | Inscreve equipe (Fluxo A) |
| `GET` | `/api/teams` | Frontend | Dashboard público (Fluxo C) |
| `POST` | `/webhooks/asaas` | Asaas | Confirma pagamento (Fluxo B) |

---

## Como rodar o backend

```powershell
# JAVA_HOME precisa apontar para o JDK 21
$env:JAVA_HOME = "C:\Program Files\Microsoft\jdk-21.0.11.10-hotspot"
.\mvnw.cmd spring-boot:run
```

Variáveis de ambiente (arquivo `.env` na raiz): `DATABASE_URL`, `DATABASE_USERNAME`,
`DATABASE_PASSWORD`, `ASAAS_API_KEY`, `ASAAS_WEBHOOK_TOKEN`, `ASAAS_PIX_KEY`, `SENDGRID_API_KEY`,
`SENDGRID_FROM_EMAIL`, `DISCORD_INVITE_URL`, `CORS_ALLOWED_ORIGINS`.

Configurações do campeonato (em `application.properties`): `app.tournament.name`,
`app.tournament.max-teams`, `app.tournament.date`, `app.tournament.prize-base`,
`app.tournament.prize-per-team`.
