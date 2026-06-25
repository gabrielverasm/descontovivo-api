# Keycloak de Produção — DescontoVivo

## Domínio e discovery

| Item              | Valor                                                                        |
|-------------------|------------------------------------------------------------------------------|
| Auth server       | `https://auth.descontovivo.com`                                              |
| Realm             | `descontovivo`                                                               |
| Issuer            | `https://auth.descontovivo.com/realms/descontovivo`                          |
| OIDC Discovery    | `https://auth.descontovivo.com/realms/descontovivo/.well-known/openid-configuration` |

## Clients

### descontovivo-ui

- Public client
- Authorization Code Flow + PKCE (S256)
- Redirect URIs: `https://descontovivo.com`, `https://descontovivo.com/*`
- Web Origins: `https://descontovivo.com`

### descontovivo-api

- Resource server — valida Bearer tokens
- Sem fluxo de login interativo

## Roles

| Role         | Herança              |
|--------------|----------------------|
| `user`       | Atribuída por padrão |
| `moderator`  | Inclui `user`        |
| `admin`      | Inclui `moderator`   |

Roles propagadas via `realm_access.roles` no access token.

## Audience

- Audience `descontovivo-api` presente no access token (via client scope default do client UI).

## Segurança do token

| Item                    | Valor     |
|-------------------------|-----------|
| Access token lifespan   | 5 minutos |
| Algoritmo de assinatura | RS256     |

## Variáveis esperadas na API

```env
OIDC_AUTH_SERVER_URL=https://auth.descontovivo.com/realms/descontovivo
OIDC_CLIENT_ID=descontovivo-api
QUARKUS_HTTP_CORS_ORIGINS=https://descontovivo.com
```

## Variáveis esperadas no Angular

| Configuração           | Valor                                              |
|------------------------|----------------------------------------------------|
| issuer                 | `https://auth.descontovivo.com/realms/descontovivo`|
| clientId               | `descontovivo-ui`                                  |
| responseType           | `code`                                             |
| scope                  | `openid profile email`                             |
| redirectUri            | `https://descontovivo.com`                         |
| postLogoutRedirectUri  | `https://descontovivo.com`                         |

## Arquivos sensíveis — nunca versionar

- `.env` e variantes
- Exports/backups do realm (`.json`, `.tar.gz`)
- Credenciais de banco, SMTP ou admin
