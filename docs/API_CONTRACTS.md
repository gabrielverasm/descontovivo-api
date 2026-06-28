# Contratos REST — DescontoVivo API

Base path:

```txt
/api/v1
```

---

## Endpoints Públicos (sem autenticação)

| Método | Endpoint                             | Descrição                        |
|--------|--------------------------------------|----------------------------------|
| GET    | /promotions                          | Listar promoções (paginado)      |
| GET    | /promotions/{slug}                   | Detalhe de uma promoção          |
| GET    | /promotions/{slug}/comments          | Comentários de uma promoção      |
| GET    | /stores                              | Listar lojas                     |
| GET    | /stores/{slug}                       | Detalhe de uma loja              |
| GET    | /system/info                         | Informações do sistema           |

### GET /promotions — Paginação e filtros

| Parâmetro    | Tipo   | Obrigatório | Descrição                                                                      |
|-------------|--------|-------------|--------------------------------------------------------------------------------|
| page        | int    | não         | Página atual (default: 0)                                                      |
| size        | int    | não         | Itens por página (default: 20, max: 100)                                       |
| store       | string | não         | Filtrar por slug da loja                                                       |
| q           | string | não         | Busca textual no título/descrição                                              |
| availability| string | não         | `AVAILABLE`, `EXPIRED`, `UNAVAILABLE`, `UNKNOWN`                               |
| sort        | string | não         | ⏳ *Futuro.* Ordenação: `recent`, `popular`, `commented`                       |
| category    | string | não         | ⏳ *Futuro.* Filtrar por slug da categoria                                     |

Resposta paginada:

```json
{
  "content": [],
  "page": 0,
  "size": 20,
  "totalElements": 150,
  "totalPages": 8
}
```

---

## Endpoints Autenticados (Bearer token obrigatório)

| Método | Endpoint                             | Descrição                        |
|--------|--------------------------------------|----------------------------------|
| GET    | /account/me                          | Perfil do usuário logado         |
| POST   | /promotions                          | Criar promoção                   |
| POST   | /uploads/promotion-image/presign     | Gerar presigned URL para upload  |
| PUT    | /promotions/{slug}/vote              | Votar na promoção                |
| DELETE | /promotions/{slug}/vote              | Remover voto                     |
| POST   | /promotions/{slug}/comments          | Comentar na promoção             |
| POST   | /comments/{id}/replies               | Responder comentário             |

### GET /account/me

Retorna dados do usuário autenticado. Requer header `Authorization: Bearer <access_token>`.

Resposta:

```json
{
  "subject": "keycloak-user-sub",
  "username": "gabriel",
  "email": "user@email.com",
  "emailVerified": true,
  "roles": ["user", "moderator"]
}
```

Características:

- Lê dados diretamente do JWT/SecurityIdentity — não persiste perfil local.
- Não retorna senha, token ou qualquer dado sensível.
- `roles` vem do `realm_access/roles` do Keycloak.

Uso pelo Angular:

- Identificar usuário logado (subject, username, email).
- Verificar se e-mail está confirmado (`emailVerified`).
- Mostrar/esconder botão "Publicar" (só se `emailVerified=true`).
- Mostrar/esconder área de moderação (se roles contém `moderator` ou `admin`).
- Bloquear ações locais quando `emailVerified=false`.

### POST /promotions — Criar promoção

Request mínimo:

```json
{
  "title": "Produto em promoção",
  "url": "https://www.amazon.com.br/dp/B0...",
  "currentPrice": 99.90,
  "imageUrl": "https://img.descontovivo.com.br/temp/promotions/2026/06/uuid.webp",
  "imageKey": "temp/promotions/2026/06/uuid.webp"
}
```

| Campo         | Obrigatório | Notas                                                         |
|---------------|-------------|---------------------------------------------------------------|
| title         | sim         | max 180 chars                                                 |
| url           | sim         | max 2048 chars                                                |
| currentPrice  | sim         | > 0                                                           |
| imageUrl      | sim         | max 2048 chars                                                |
| imageKey      | sim         | max 200 chars; chave do objeto no R2                          |
| description   | não         | max 2000 chars; se omitido, usa title como fallback           |
| originalPrice | não         | preço anterior (riscado)                                      |
| couponCode    | não         | max 80 chars                                                  |
| storeSlug     | não         | se omitido, infere pela URL ou usa "loja-nao-identificada"    |

Resposta: `201` com `PromotionDetailResponse`. Status inicial: `PENDING_REVIEW`.

### POST /uploads/promotion-image/presign — Gerar URL de upload

Request:

```json
{
  "contentType": "image/webp",
  "fileSize": 1048576
}
```

Resposta:

```json
{
  "uploadUrl": "https://r2.cloudflarestorage.com/...",
  "publicUrl": "https://img.descontovivo.com.br/temp/promotions/2026/06/uuid.webp",
  "objectKey": "temp/promotions/2026/06/uuid.webp",
  "expiresInSeconds": 300
}
```

Fluxo Angular:
1. Chamar presign → receber `uploadUrl`, `publicUrl`, `objectKey`
2. Fazer PUT do arquivo .webp no `uploadUrl` com header `Content-Type: image/webp`
3. Enviar `publicUrl` como `imageUrl` e `objectKey` como `imageKey` no POST /promotions

> **Nota:** Imagens ficam em `temp/promotions/` até serem movidas por job futuro. Imagens não referenciadas com mais de 7 dias serão limpas automaticamente (TODO).

### PUT /promotions/{slug}/vote

```json
{ "type": "LIKE" }
```

Valores aceitos: `LIKE`, `DISLIKE`.

### DELETE /promotions/{slug}/vote

Remove o voto do usuário autenticado.

---

## Endpoints de Moderação (roles: `moderator` ou `admin`)

| Método | Endpoint                             | Descrição                        |
|--------|--------------------------------------|----------------------------------|
| GET    | /moderation/promotions               | Listar promoções para moderar    |
| PATCH  | /moderation/promotions/{id}          | Ação de moderação na promoção    |
| PATCH  | /moderation/comments/{id}            | Ação de moderação no comentário  |

---

## Endpoints Admin (role `admin` ou header `X-Admin-Import-Token`)

| Método | Endpoint                             | Descrição                        |
|--------|--------------------------------------|----------------------------------|
| POST   | /admin/promotions/import             | Importar promoções via JSON      |

### POST /admin/promotions/import?dryRun=false

Autorização: role `admin` via JWT **ou** header `X-Admin-Import-Token` com valor configurado em `ADMIN_IMPORT_TOKEN`.

Query params:

| Parâmetro | Tipo    | Default | Descrição                                    |
|-----------|---------|---------|----------------------------------------------|
| dryRun    | boolean | false   | Se true, valida sem gravar no banco          |

Request body:

```json
{
  "batchId": "initial-2026-06-26-001",
  "items": [
    {
      "sourceId": "<identificador-unico>",
      "title": "<titulo>",
      "description": "<descricao>",
      "marketplace": "<MARKETPLACE>",
      "storeName": "<nome-loja>",
      "sellerName": "<vendedor>",
      "soldBy": "<vendido-por>",
      "deliveredBy": "<entregue-por>",
      "productUrl": "<url-produto>",
      "imageUrl": "<url-imagem>",
      "currentPrice": 0.01,
      "originalPrice": null,
      "coupon": null,
      "category": "<CATEGORIA>",
      "publishAt": "2026-06-26T21:30:00-03:00",
      "verifiedAt": "2026-06-26T21:00:00-03:00"
    }
  ]
}
```

Regras:

- `batchId` opcional (gerado automaticamente se ausente)
- `sourceId` obrigatório e único — chave de deduplicação
- `publishAt` opcional — se ausente, usa timestamp do início da importação
- Promoções com `publishAt` futuro ficam salvas mas invisíveis na listagem pública até a data/hora
- Se `sourceId` já existir no banco, item é **pulado** (skipped)
- Se `productUrl` normalizado já existir, item é **pulado**
- Store é criada automaticamente se não existir

Response:

```json
{
  "batchId": "initial-2026-06-26-001",
  "dryRun": false,
  "created": 18,
  "skipped": 2,
  "errors": [
    {
      "sourceId": "<id-item>",
      "field": "productUrl",
      "message": "productUrl obrigatório"
    }
  ]
}
```

Reversibilidade:

```sql
-- Verificar o que será removido
SELECT id, title, source_id, batch_id FROM promotion
WHERE source = 'ADMIN_JSON_IMPORT' AND batch_id = '<batchId>';

-- Remover dependências (votos e comentários, se existirem)
DELETE FROM promotion_vote WHERE promotion_id IN (
    SELECT id FROM promotion WHERE source = 'ADMIN_JSON_IMPORT' AND batch_id = '<batchId>'
);
DELETE FROM promotion_comment WHERE promotion_id IN (
    SELECT id FROM promotion WHERE source = 'ADMIN_JSON_IMPORT' AND batch_id = '<batchId>'
);

-- Remover promoções
DELETE FROM promotion WHERE source = 'ADMIN_JSON_IMPORT' AND batch_id = '<batchId>';
```

Campos persistidos por item: `marketplace`, `sellerName`, `soldBy`, `deliveredBy`, `category`, `sourceId`, `batchId`, `source`, `authorUsername`, `publishAt`, `verifiedAt`.

---
### PATCH /moderation/promotions/{id}

Ações: `APPROVE`, `REJECT`, `REMOVE`, `EDIT`.

```json
{ "action": "REJECT", "reason": "Link inválido" }
```

Edição (campos flat no DTO):

```json
{ "action": "EDIT", "reason": "Correção de título", "title": "Título corrigido" }
```

### PATCH /moderation/comments/{id}

Ação: `REMOVE` (reason obrigatório).

```json
{ "action": "REMOVE", "reason": "Comentário ofensivo" }
```

---

## Contrato para Integração com Angular

### Regras de Autorização

| Cenário                                  | Comportamento da API                          |
|------------------------------------------|-----------------------------------------------|
| Sem token                                | Endpoints protegidos retornam **401**         |
| Token válido, sem role de moderação      | Endpoints de moderação retornam **403**       |
| `emailVerified=false`                    | Não deve publicar, votar nem comentar (**403**)|
| Roles `moderator` / `admin`              | Liberam área de moderação                     |

O Angular deve sempre enviar:

```txt
Authorization: Bearer <access_token>
```

### Tratamento de Erros HTTP

| Código | Significado                                    | Ação sugerida no frontend               |
|--------|------------------------------------------------|------------------------------------------|
| 400    | Payload inválido / validação (`@Size`, etc.)  | Exibir mensagens de validação            |
| 401    | Não autenticado ou sessão expirada             | Redirecionar para login                  |
| 403    | Sem permissão ou e-mail não verificado         | Exibir mensagem de acesso negado         |
| 404    | Recurso não encontrado                         | Página 404 ou mensagem                  |
| 409    | Conflito de regra de negócio                   | Informar conflito ao usuário             |
| 422    | Valor semanticamente inválido                  | Exibir erro específico                   |
| 500    | Erro inesperado                                | Mensagem genérica + retry                |

### URLs Úteis (Desenvolvimento)

| Recurso       | URL                                    |
|---------------|----------------------------------------|
| API base      | http://localhost:8080/api/v1           |
| Swagger UI    | http://localhost:8080/q/swagger-ui/    |
| OpenAPI spec  | http://localhost:8080/q/openapi        |
| Health check  | http://localhost:8080/q/health         |
| Keycloak      | http://localhost:8082/                 |

### Variáveis de Configuração para Integração

**Backend (API):**

| Variável                      | Descrição                              |
|-------------------------------|----------------------------------------|
| `OIDC_AUTH_SERVER_URL`        | URL do realm Keycloak                  |
| `OIDC_CLIENT_ID`              | Client ID da API (`descontovivo-api`)  |
| `QUARKUS_HTTP_CORS_ORIGINS`   | Origens CORS permitidas para o SPA     |

**Angular (SPA) — Desenvolvimento:**

| Configuração                  | Valor                                   |
|-------------------------------|----------------------------------------|
| issuer / auth server URL      | `http://localhost:8082/realms/descontovivo` |
| client id                     | `descontovivo-ui` (public client)      |
| redirect URI                  | `http://localhost:4200`                |
| post logout redirect URI      | `http://localhost:4200`                |
| scopes                        | `openid profile email`                 |
| response type                 | `code` (Authorization Code + PKCE)     |

**Angular (SPA) — Produção:**

| Configuração                  | Valor                                              |
|-------------------------------|----------------------------------------------------|
| issuer / auth server URL      | `https://auth.descontovivo.com/realms/descontovivo`|
| client id                     | `descontovivo-ui` (public client)                  |
| redirect URI                  | `https://descontovivo.com`                |
| post logout redirect URI      | `https://descontovivo.com`                         |
| scopes                        | `openid profile email`                             |
| response type                 | `code` (Authorization Code + PKCE)                 |

---

## Checklist — API Pronta para Angular

- [ ] Testes passando (`./mvnw clean test`)
- [ ] Swagger UI validado em dev
- [ ] `GET /api/v1/account/me` disponível e retornando dados corretos
- [ ] Keycloak configurado com realm e client SPA
- [ ] CORS liberando origem do frontend (`http://localhost:4200`)
- [ ] Angular enviando `Authorization: Bearer <token>` em requisições autenticadas
- [ ] Frontend tratando respostas 401, 403 e 422
- [ ] Swagger UI e OpenAPI desligados em produção (`%prod`)
- [ ] Autenticação via Authorization Code Flow + PKCE
- [ ] Registro de usuário feito no Keycloak (sem endpoint próprio na API)

---

## Observações Gerais

- Registro e login são feitos exclusivamente no Keycloak; o Angular gerencia o fluxo OIDC.
- A API não possui login próprio, não guarda senha e não emite tokens.
- `/account/me` apenas lê claims do JWT — não retorna token nem senha.
- O login é realizado pelo Keycloak/OIDC com Authorization Code Flow + PKCE no frontend.
