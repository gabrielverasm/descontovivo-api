# Arquitetura

## Decisão principal

O DescontoVivo API será construído como um monólito modular com arquitetura hexagonal pragmática.

A prioridade é manter o backend simples de executar, fácil de testar e organizado o suficiente para evoluir sem virar um CRUD acoplado ao framework.

## Stack

- Java 25 LTS
- Quarkus 3.33.x LTS
- PostgreSQL
- Flyway
- Hibernate ORM with Panache
- Keycloak/OIDC com SPA Authorization Code + PKCE

## Módulos

```txt
br.com.descontovivo
├── promotion
├── engagement
├── store
├── moderation
├── account
└── shared
```

## Separação de responsabilidades

```txt
Resource
→ recebe HTTP, valida Request DTO e chama service

Request DTO
→ representa entrada da API

Service
→ orquestra caso de uso e regras de aplicação

Domain / Value Object
→ concentra regras, validações e tipos fortes

Repository
→ executa consultas e persistência com Panache

Entity
→ representa o modelo relacional/JPA

Response DTO
→ representa saída da API para o frontend
```

## Fluxo de camadas

```txt
Resource → Service → Repository
```

- Resources: anotações REST/segurança, recebem DTO, delegam ao service, retornam response.
- Services (@ApplicationScoped): regras de negócio, validações, transações (@Transactional).
- Repositories: consultas e persistência com Panache.

## Identificadores

- UUID será usado como identificador interno.
- Slug será usado como identificador público nas rotas de promoção, loja e categoria.

## Status de promoção

```txt
PENDING_REVIEW
PUBLISHED
REJECTED
REMOVED
```

## Disponibilidade da oferta

```txt
AVAILABLE
EXPIRED
UNAVAILABLE
UNKNOWN
```

Status e disponibilidade são conceitos diferentes.

Uma promoção publicada pode continuar visível mesmo quando estiver expirada ou indisponível.