# Contratos REST Iniciais

Base path:

```txt
/api/v1
```

## Promoções públicas

```txt
GET /promotions
GET /promotions/{slug}
GET /promotions/{slug}/comments
GET /promotions/{slug}/related
```

### GET /promotions — Paginação e filtros

Parâmetros de query:

| Parâmetro   | Tipo    | Obrigatório | Descrição                                      |
|-------------|---------|-------------|------------------------------------------------|
| page        | int     | não         | Página atual (default: 0)                      |
| size        | int     | não         | Itens por página (default: 20, max: 100)       |
| sort        | string  | não         | Ordenação: `recent`, `popular`, `commented`    |
| store       | string  | não         | Filtrar por slug da loja                       |
| category    | string  | não         | Filtrar por slug da categoria                  |
| q           | string  | não         | Busca textual no título/descrição              |
| availability| string  | não         | Filtrar por disponibilidade: `AVAILABLE`, `EXPIRED`, `UNAVAILABLE`, `UNKNOWN` |

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

## Promoções autenticadas

```txt
POST   /promotions
PUT    /promotions/{slug}/vote
DELETE /promotions/{slug}/vote
POST   /promotions/{slug}/comments
```

### PUT /promotions/{slug}/vote

Registra ou altera o voto do usuário autenticado na promoção.

```json
{ "type": "LIKE" }
```

Valores aceitos: `LIKE`, `DISLIKE`.

### DELETE /promotions/{slug}/vote

Remove o voto do usuário autenticado na promoção.

## Comentários

```txt
POST /comments/{id}/replies
PUT  /comments/{id}/vote
DELETE /comments/{id}/vote
```

## Moderação

```txt
GET   /moderation/promotions
PATCH /moderation/promotions/{id}
PATCH /moderation/comments/{id}
```

### PATCH /moderation/promotions/{id}

Ações possíveis: `APPROVE`, `REJECT`, `REMOVE`, `EDIT`.

```json
{
  "action": "REJECT",
  "reason": "Link inválido"
}
```

Para edição:

```json
{
  "action": "EDIT",
  "reason": "Correção de título",
  "fields": {
    "title": "Título corrigido"
  }
}
```

### PATCH /moderation/comments/{id}

Ação possível: `REMOVE` (reason obrigatório).

```json
{
  "action": "REMOVE",
  "reason": "Comentário ofensivo"
}
```

## Lojas

```txt
GET /stores
GET /stores/{slug}
```

## Conta

```txt
GET /account/me
```

### GET /account/me

Retorna o perfil local do usuário autenticado.

Na primeira requisição autenticada, a API cria o perfil local automaticamente a partir das claims do JWT (sub, email, name).

Observações:

- `POST /auth/register` foi removido — o registro é feito no Keycloak e o perfil local é criado sob demanda.
- O login será realizado pelo Keycloak/OIDC com Authorization Code Flow + PKCE no frontend.
- A API não terá login próprio com senha no MVP.
