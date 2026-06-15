# DescontoVivo API

Backend do DescontoVivo, um portal de promoções com foco em comunidade, contexto e sinais de confiança.

Este repositório será a API principal consumida pelo frontend `descontovivo-ui`.

## Objetivo

Construir uma API limpa, testável e evolutiva para:

- listar promoções;
- detalhar promoções;
- publicar promoções;
- moderar publicações;
- registrar votos;
- registrar comentários;
- organizar lojas e categorias;
- integrar autenticação com Keycloak/OIDC.

## Stack planejada

- Java 25 LTS
- Quarkus 3.33.x LTS
- PostgreSQL
- Flyway
- Hibernate ORM with Panache
- Quarkus REST / Jackson
- Hibernate Validator
- SmallRye OpenAPI
- SmallRye Health
- Keycloak/OIDC
- JUnit 5
- RestAssured
- Dev Services/Testcontainers

## Arquitetura planejada

- Monólito modular
- Arquitetura hexagonal pragmática
- Request DTOs para entrada da API
- Response DTOs para saída da API
- Domain Value Objects para regras e tipos fortes
- Entities JPA/Panache separadas do domínio
- Panache Repository para persistência

## Módulos iniciais

- `promotion`
- `engagement`
- `store`
- `moderation`
- `account`
- `shared`

## Status

Projeto em fase de desenho arquitetural.

Antes de iniciar a implementação Quarkus, serão definidos:

- contratos REST;
- regras de negócio do MVP;
- decisões arquiteturais;
- estrutura de pacotes;
- estratégia de autenticação;
- estratégia de testes.