# Notificações SSE — DescontoVivo API

## Visão geral

A API expõe três endpoints de Server-Sent Events (SSE) para notificações leves em quase tempo real:

- **Stream público**: contadores de promoções publicadas (sem autenticação).
- **Stream moderação**: contadores de promoções pendentes de moderação (requer role `admin` ou `moderator`).
- **Stream admin**: contadores de moderação e solicitações de dados (requer role `admin`).

Os streams emitem snapshots periódicos — não são eventos transacionais instantâneos no momento do insert/update.

---

## Endpoints

### GET `/api/v1/events/public/stream`

| Propriedade     | Valor                       |
|-----------------|-----------------------------|
| Content-Type    | `text/event-stream`         |
| Autenticação    | Nenhuma (`@PermitAll`)      |
| Frequência      | Snapshot a cada 30 segundos |
| Dados sensíveis | Nenhum                      |

**Eventos emitidos:**

#### `heartbeat`

```
event: heartbeat
data: {"timestamp":"2026-07-03T22:00:00Z"}
```

#### `promotions`

```
event: promotions
data: {"publishedCount":123,"latestPublishedAt":"2026-07-03T21:59:00Z"}
```

| Campo             | Tipo               | Descrição                                          |
|-------------------|--------------------|----------------------------------------------------|
| `publishedCount`  | long               | Total de promoções publicadas e visíveis agora     |
| `latestPublishedAt` | ISO 8601 / null  | Timestamp da última promoção publicada (campo `published_at`) |

---

### GET `/api/v1/events/moderation/stream`

| Propriedade     | Valor                                  |
|-----------------|----------------------------------------|
| Content-Type    | `text/event-stream`                    |
| Autenticação    | Bearer token, role `admin` ou `moderator` |
| Frequência      | Snapshot a cada 30 segundos            |
| Dados sensíveis | Nenhum (apenas contadores)             |

**Eventos emitidos:**

#### `heartbeat`

```
event: heartbeat
data: {"timestamp":"2026-07-03T22:00:00Z"}
```

#### `moderation-promotions`

```
event: moderation-promotions
data: {"pendingCount":4}
```

| Campo          | Tipo | Descrição                                      |
|----------------|------|------------------------------------------------|
| `pendingCount` | long | Promoções com status `PENDING_REVIEW`          |

**Uso na UI:** Este é o endpoint que a UI deve consumir para exibir o badge "Moderar promoções" no menu de moderação — tanto para admins quanto moderadores.

---

### GET `/api/v1/events/admin/stream`

| Propriedade     | Valor                              |
|-----------------|------------------------------------|
| Content-Type    | `text/event-stream`                |
| Autenticação    | Bearer token, role `admin`         |
| Frequência      | Snapshot a cada 30 segundos        |
| Dados sensíveis | Nenhum (apenas contadores)         |

**Eventos emitidos:**

#### `heartbeat`

```
event: heartbeat
data: {"timestamp":"2026-07-03T22:00:00Z"}
```

#### `moderation-promotions`

```
event: moderation-promotions
data: {"pendingCount":4}
```

| Campo          | Tipo | Descrição                                      |
|----------------|------|------------------------------------------------|
| `pendingCount` | long | Promoções com status `PENDING_REVIEW`          |

#### `admin-data-requests`

```
event: admin-data-requests
data: {"openCount":2}
```

| Campo       | Tipo | Descrição                                           |
|-------------|------|-----------------------------------------------------|
| `openCount` | long | Solicitações com status `PENDING` ou `IN_REVIEW`    |

**Uso na UI:** Este é o endpoint que a UI deve consumir para exibir o badge "Solicitações de dados" — exclusivo para admins.

---

## Segurança

### Princípios

- Nenhum dado sensível é transmitido via SSE (e-mail, nome, userSubject, detalhes).
- Eventos contêm apenas contadores agregados.
- Moderation stream exige Bearer token com role `admin` ou `moderator` no header `Authorization`.
- Admin stream exige Bearer token com role `admin` no header `Authorization`.
- **Não** usamos token em query param (`?token=...`). Isso é inseguro.

### Matriz de acesso

| Endpoint                   | `user` | `moderator` | `admin` |
|----------------------------|--------|-------------|---------|
| `/events/public/stream`    | ✅     | ✅          | ✅      |
| `/events/moderation/stream`| ❌ 403 | ✅          | ✅      |
| `/events/admin/stream`     | ❌ 403 | ❌ 403      | ✅      |

### Limitação do EventSource nativo

O `EventSource` nativo do browser **não suporta** headers customizados (como `Authorization: Bearer ...`).

**Consequência:**

| Endpoint                     | Consumo no frontend                                          |
|------------------------------|--------------------------------------------------------------|
| `/events/public/stream`      | `new EventSource(url)` — funciona diretamente              |
| `/events/moderation/stream`  | `fetch()` com `ReadableStream` ou lib como `@microsoft/fetch-event-source` |
| `/events/admin/stream`       | `fetch()` com `ReadableStream` ou lib como `@microsoft/fetch-event-source` |

A UI deve usar o access token atual do Keycloak/AuthService via header Authorization com fetch streaming. **Nunca colocar token em query string.**

---

## Configuração

| Propriedade                           | Default | Descrição                        |
|---------------------------------------|---------|----------------------------------|
| `notification.sse.interval-seconds`   | 30      | Intervalo entre emissões (seg)   |
| `NOTIFICATION_SSE_INTERVAL_SECONDS`   | 30      | Override via variável de ambiente |

---

## Queries e contadores

| Contador            | Query                                                                 | Tabela/Campo                   |
|---------------------|-----------------------------------------------------------------------|--------------------------------|
| `publishedCount`    | `COUNT WHERE status = 'PUBLISHED' AND publish_at <= now()`            | `promotion.status`, `promotion.publish_at` |
| `latestPublishedAt` | `MAX(published_at) WHERE status = 'PUBLISHED' AND publish_at <= now()` | `promotion.published_at`       |
| `pendingCount`      | `COUNT WHERE status = 'PENDING_REVIEW'`                               | `promotion.status`             |
| `openCount`         | `COUNT WHERE status IN ('PENDING', 'IN_REVIEW')`                      | `account_data_request.status`  |

Todas as queries usam `COUNT`/operações agregadas. Nenhuma lista é carregada para memória.

---

## Limitações da v1

1. **Snapshot periódico, não push transacional**: o frontend é notificado a cada ~30s, não no exato momento do insert.
2. **Sem deduplicação**: o mesmo snapshot pode ser emitido múltiplas vezes se nada mudar.
3. **Sem `Last-Event-ID`**: reconexão reinicia do zero (o frontend deve usar `EventSource` com reconexão automática para stream público).
4. **Sem compressão**: SSE não deve ser comprimido por proxies intermediários.

---

## Infraestrutura

### DNS e proxy

`api.descontovivo.com` está atualmente como **DNS Only** no Cloudflare (sem proxy/orange cloud). Isso significa que o SSE vai direto para o Hetzner sem interferência.

**Se futuramente ativar o proxy Cloudflare:**

- Revisar timeout de conexões longas (Cloudflare pode cortar após 100s idle).
- Configurar `X-Accel-Buffering: no` se usar nginx como reverse proxy.
- Desabilitar compressão para `text/event-stream`.
- Considerar Cloudflare Enterprise timeout settings.

### Conexões abertas

Cada cliente SSE mantém uma conexão HTTP aberta. Em escala:

- Monitorar número de conexões abertas.
- Considerar limite via configuração Vert.x (`quarkus.http.limits.max-connections`).
- O Quarkus REST é event-loop based (Vert.x), não bloqueamos threads — conexões abertas são eficientes.

---

## Como a UI deve consumir

### Stream público (sem auth)

```typescript
const source = new EventSource('https://api.descontovivo.com/api/v1/events/public/stream');

source.addEventListener('promotions', (event) => {
  const data = JSON.parse(event.data);
  // data.publishedCount, data.latestPublishedAt
  updateBadge(data.publishedCount);
});

source.addEventListener('heartbeat', (event) => {
  // Connection alive
});

source.onerror = () => {
  // EventSource reconecta automaticamente
};
```

### Stream moderação (admin ou moderator)

```typescript
import { fetchEventSource } from '@microsoft/fetch-event-source';

fetchEventSource('https://api.descontovivo.com/api/v1/events/moderation/stream', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  onmessage(event) {
    if (event.event === 'moderation-promotions') {
      const data = JSON.parse(event.data);
      updateModerationBadge(data.pendingCount);
    }
  },
  onerror(err) {
    // Reconectar com novo token se 401
  }
});
```

### Stream admin (admin-only)

```typescript
import { fetchEventSource } from '@microsoft/fetch-event-source';

fetchEventSource('https://api.descontovivo.com/api/v1/events/admin/stream', {
  headers: {
    'Authorization': `Bearer ${accessToken}`
  },
  onmessage(event) {
    if (event.event === 'moderation-promotions') {
      const data = JSON.parse(event.data);
      updateModerationBadge(data.pendingCount);
    }
    if (event.event === 'admin-data-requests') {
      const data = JSON.parse(event.data);
      updateDataRequestBadge(data.openCount);
    }
  },
  onerror(err) {
    // Reconectar com novo token se 401
  }
});
```

### Qual stream usar para cada badge

| Badge na UI                 | Endpoint recomendado           | Role mínima |
|-----------------------------|--------------------------------|-------------|
| "Moderar promoções"         | `/events/moderation/stream`    | `moderator` |
| "Solicitações de dados"     | `/events/admin/stream`         | `admin`     |

> **Admins** podem usar o moderation stream OU o admin stream para o badge de moderação.
> A UI deve escolher o moderation stream para moderadores e, opcionalmente, o admin stream para admins (se quiser também o badge de data requests no mesmo stream).

### Atualizar badges e título

```typescript
// Exemplo: atualizar título da aba
document.title = pendingCount > 0
  ? `(${pendingCount}) DescontoVivo Admin`
  : 'DescontoVivo Admin';
```

---

## Evolução futura (v2+)

- Push transacional via CDI Events / Vert.x EventBus ao publicar promoção.
- `Last-Event-ID` para reconexão sem perda.
- Rate limiting por IP para stream público.
- Métricas Micrometer: conexões ativas, snapshots emitidos.
