# DescontoVivo API — Resumo para Portfólio

## Título

DescontoVivo API — Backend de portal público de promoções com moderação e comunidade.

## Problema resolvido

Criar uma API REST robusta para um portal de promoções onde usuários publicam ofertas, moderadores aprovam conteúdo e imagens são armazenadas de forma segura — sem depender de hotlinks externos.

## Stack

- Java 25 LTS
- Quarkus 3.33.x LTS
- PostgreSQL + Flyway
- Hibernate ORM with Panache
- Keycloak/OIDC (JWT Bearer)
- Cloudflare R2 (armazenamento S3-compatible)
- SmallRye OpenAPI + Health
- JUnit 5 + RestAssured + Testcontainers

## Principais decisões técnicas

- Monólito modular com separação hexagonal pragmática (DTOs, domain, entities).
- Imagem final sempre no R2 próprio — import admin baixa, processa e salva.
- Presigned URL para upload do SPA; API promove de `temp/` para destino final.
- SSRF protection no download de imagens externas.
- Moderação com roles e audit trail de todas as ações.
- Slug como identificador público (SEO-friendly).
- Status de promoção separado de disponibilidade da oferta.
- Testes com QuarkusTest + Dev Services (Testcontainers) — sem infra externa.

## Segurança

- JWT Bearer Token validado pelo Quarkus OIDC.
- Roles `user`, `moderator`, `admin` via Keycloak.
- Endpoints admin protegidos por role ou token dedicado.
- Edição/moderação não aceita URL externa como imagem — apenas `imageKey` temporário.
- CORS restrito às origens do frontend.

## Screenshots

Pendentes — a ser adicionado após deploy final com frontend completo.

## Repositórios

- API: [github.com/gabrielverasm/descontovivo-api](https://github.com/gabrielverasm/descontovivo-api)
- UI: [github.com/gabrielverasm/descontovivo-ui](https://github.com/gabrielverasm/descontovivo-ui)

## Status

MVP funcional e publicado. Worker/crawler e integrações avançadas no roadmap.
