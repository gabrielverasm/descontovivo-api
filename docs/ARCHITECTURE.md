# Arquitetura

## Decisão principal

Monólito modular com arquitetura hexagonal pragmática.
Prioridade: simples de executar, fácil de testar, organizado para evoluir sem virar CRUD acoplado ao framework.

## Stack

- Java 25 LTS
- Quarkus 3.33.x LTS
- PostgreSQL
- Flyway
- Hibernate ORM with Panache
- Keycloak/OIDC com Authorization Code + PKCE (SPA)
- Cloudflare R2 (armazenamento de imagens)
- SmallRye OpenAPI / Health

## Módulos

```txt
br.com.descontovivo
├── promotion       → CRUD, listagem pública, slug, paginação, busca
├── engagement      → votos e comentários
├── store           → lojas (auto-criadas no import)
├── moderation      → aprovação, rejeição, remoção, edição com audit
├── account         → perfil via JWT (sem persistência local de senha)
├── upload          → presign, promoção de imagem temp→final, R2
└── shared          → utilitários, exceptions, mappers, image processing
```

## Separação de responsabilidades

```txt
Resource       → recebe HTTP, valida Request DTO, chama service
Request DTO    → representa entrada da API
Service        → orquestra caso de uso e regras de aplicação
Domain / VO    → regras, validações e tipos fortes
Repository     → consultas e persistência com Panache
Entity         → modelo relacional/JPA
Response DTO   → saída da API para o frontend
```

## Fluxo de camadas

```txt
Resource → Service → Repository
```

- Resources: anotações REST/segurança, recebem DTO, delegam ao service.
- Services (`@ApplicationScoped`): regras de negócio, validações, `@Transactional`.
- Repositories: persistência com Panache.

## Upload e imagens (R2)

- Upload via presigned URL: SPA envia arquivo WebP para `temp/promotions/`.
- Ao criar/editar promoção, API promove imagem de `temp/` para `promotions/`.
- Import admin baixa imagem da URL de origem, processa (resize 300x300, WebP) e salva no R2.
- Backfill admin migra imagens externas antigas para R2.
- Imagem final sempre reside no R2 — nenhuma promoção fica com hotlink externo.
- Pipeline de processamento: validação → resize contain → padding branco → WebP → upload R2.

## Segurança

- JWT Bearer Token validado pelo Quarkus OIDC.
- Roles: `user`, `moderator`, `admin` via `realm_access.roles`.
- Endpoints públicos sem token; protegidos exigem Bearer.
- Import/backfill admin: role `admin` ou header `X-Admin-Import-Token`.
- SSRF protection no download de imagens: bloqueio de localhost, IPs privados, link-local.
- Edição/moderação não aceita `imageUrl` externo — apenas `imageKey` temporário.
- CORS restrito apenas às origens do frontend.

## Identificadores

- UUID como identificador interno.
- Slug como identificador público nas rotas (SEO-friendly).

## Status de promoção

```txt
PENDING_REVIEW → PUBLISHED → REMOVED
             └→ REJECTED
```

## Disponibilidade da oferta

```txt
AVAILABLE | EXPIRED | UNAVAILABLE | UNKNOWN
```

Status e disponibilidade são conceitos independentes. Uma promoção publicada permanece visível mesmo quando a oferta expira.

## Decisões de MVP

- Sem worker/crawler de ofertas (roadmap).
- Sem job automático de limpeza de imagens órfãs no R2 (roadmap).
- Import admin é executado manualmente via endpoint.
- Backfill admin é executado em lotes manuais.
- Perfil do usuário lido apenas do JWT, sem tabela local de usuários.
