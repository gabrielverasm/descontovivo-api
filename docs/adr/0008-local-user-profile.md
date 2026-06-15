# ADR-0008: Perfil local do usuário autenticado via Keycloak

## Status

Aceita

## Contexto

A autenticação é delegada ao Keycloak (ADR-0006), mas a API precisa de um registro local do usuário para associar autoria de promoções, votos, comentários e ações de moderação.

## Decisão

- A API mantém uma tabela de perfil local (`user_profile` ou equivalente).
- O perfil é criado automaticamente na primeira requisição autenticada (provisão sob demanda).
- O identificador é o `sub` (subject) do JWT.
- Claims extraídas na criação: `sub`, `email`, `preferred_username`, `name`.
- O endpoint `GET /account/me` retorna o perfil local e o cria se não existir.
- `POST /auth/register` é removido — não há registro manual na API.

## Consequências

- Não há necessidade de fluxo de registro na API.
- O perfil local é sempre consistente com o IdP.
- Relações de autoria usam o ID local vinculado ao `sub` do Keycloak.
- Se dados do perfil mudarem no Keycloak, a API pode atualizar na próxima autenticação.
