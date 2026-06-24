# DescontoVivo API

Backend do DescontoVivo — portal de promoções com foco em comunidade, contexto e sinais de confiança.

API principal consumida pelo frontend `descontovivo-ui`.

## Stack

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
- JUnit 5 / RestAssured / Testcontainers (Dev Services)

## Arquitetura

- Monólito modular (hexagonal pragmático)
- Módulos atuais: `promotion`, `engagement`, `store`, `moderation`, `shared`
- Módulo planejado: `account`
- Request/Response DTOs, Domain Value Objects, Entities JPA, Panache Repositories

## Desenvolvimento local

### Com Docker Compose (Keycloak + PostgreSQL)

```bash
cp .env.example .env
docker compose up -d
# API disponível em http://localhost:8080
```

### Modo dev

Em dev, PostgreSQL pode ser provisionado por Dev Services/Testcontainers; Keycloak local pode ser iniciado via Docker Compose em http://localhost:8082.

```bash
./mvnw quarkus:dev
```

### URLs úteis em dev

| Recurso       | URL                                      |
|---------------|------------------------------------------|
| API base      | http://localhost:8080/api/v1/promotions   |
| Swagger UI    | http://localhost:8080/q/swagger-ui/       |
| OpenAPI spec  | http://localhost:8080/q/openapi           |
| Health check  | http://localhost:8080/q/health            |
| Keycloak      | http://localhost:8082/ (Docker Compose)   |

## Testes

```bash
./mvnw clean test
```

Os testes usam `@QuarkusTest` + Dev Services (Testcontainers), não necessitam infraestrutura externa.

Cobertura inclui:

- CRUD de promoções (auth, 401, 403, 409)
- Votação e comentários
- Moderação de promoções e comentários
- Validação de constraints (`@Size`, `@NotBlank`) → HTTP 400
- `IllegalArgumentExceptionMapper` → HTTP 422
- Stores e system info

## Execução em produção

### Variáveis de ambiente obrigatórias

| Variável                            | Descrição                          |
|-------------------------------------|------------------------------------|
| `QUARKUS_DATASOURCE_JDBC_URL`       | JDBC URL do PostgreSQL             |
| `QUARKUS_DATASOURCE_USERNAME`       | Usuário do banco                   |
| `QUARKUS_DATASOURCE_PASSWORD`       | Senha do banco                     |
| `OIDC_AUTH_SERVER_URL`              | URL do realm Keycloak              |
| `OIDC_CLIENT_ID`                    | Client ID OIDC                     |
| `QUARKUS_HTTP_CORS_ORIGINS`         | Origens CORS permitidas            |

> **Override avançado:** também é possível usar `QUARKUS_OIDC_AUTH_SERVER_URL` e `QUARKUS_OIDC_CLIENT_ID` diretamente, que sobrescrevem qualquer valor do application.properties.

### Build e deploy

```bash
docker build -t descontovivo-api .
docker run -p 8080:8080 --env-file .env descontovivo-api
```

### Comportamento em produção

- Swagger UI: **desligado**
- OpenAPI endpoint: **desligado**
- Health check: **ativo** em `/q/health`

## Checklist de produção

- [ ] Datasource configurado via variáveis de ambiente
- [ ] OIDC/Keycloak configurado e acessível
- [ ] CORS restrito apenas a origens do frontend
- [ ] Swagger UI e OpenAPI desligados (`%prod`)
- [ ] Secrets fora do repositório Git (usar `.env` ou secrets manager)
- [ ] Health check ativo (`/q/health`)
- [ ] Migrations Flyway aplicadas automaticamente no startup
- [ ] Testes passando antes de deploy (`./mvnw clean test`)
