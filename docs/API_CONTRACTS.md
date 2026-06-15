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

## Promoções autenticadas

```txt
POST /promotions
POST /promotions/{slug}/votes
POST /promotions/{slug}/comments
```

## Comentários

```txt
POST /comments/{id}/replies
POST /comments/{id}/votes
```

## Moderação

```txt
GET   /moderation/promotions
PATCH /moderation/promotions/{id}
```

## Lojas

```txt
GET /stores
GET /stores/{slug}
```

## Conta/autenticação

```txt
GET  /account/me
POST /auth/register
```

Observação:

O login será realizado pelo Keycloak/OIDC com Authorization Code Flow + PKCE no frontend.

A API não terá login próprio com senha no MVP.