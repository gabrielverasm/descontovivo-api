# ADR-0006: Usar Keycloak/OIDC com Authorization Code + PKCE

## Status

Aceita

## Contexto

O frontend `descontovivo-ui` será uma SPA Angular.

A API não deve armazenar senha nem implementar autenticação própria desnecessária.

## Decisão

Usaremos Keycloak/OIDC com Authorization Code Flow + PKCE no frontend.

A API validará JWT Bearer Token nos endpoints protegidos.

## Consequências

- Autenticação delegada para um Identity Provider.
- Melhor separação entre identidade e regras de negócio.
- API mais segura e menos acoplada a senha/login.
- Será necessário manter perfil local do usuário na API para autoria, votos, comentários e moderação.