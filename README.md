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
- Módulos atuais: `promotion`, `engagement`, `store`, `moderation`, `account`, `upload`, `shared`
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
- Import admin com download de imagem para R2 (SSRF protection, validação, dryRun)
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
| `R2_ENDPOINT`                       | Endpoint S3-compatible do R2       |
| `R2_ACCESS_KEY_ID`                  | Access key do R2                   |
| `R2_SECRET_ACCESS_KEY`              | Secret key do R2                   |
| `R2_BUCKET`                         | Nome do bucket (`descontovivo-promotions`) |
| `R2_PUBLIC_BASE_URL`                | URL pública das imagens            |

> **Override avançado:** também é possível usar `QUARKUS_OIDC_AUTH_SERVER_URL` e `QUARKUS_OIDC_CLIENT_ID` diretamente, que sobrescrevem qualquer valor do application.properties.

#### Variáveis opcionais (image processing)

| Variável                            | Default | Descrição                               |
|-------------------------------------|---------|-------------------------------------------|
| `IMAGE_MAX_UPLOAD_BYTES`            | 2097152 | Tamanho máximo do download (2MB)          |
| `IMAGE_TARGET_SIZE`                 | 300     | Dimensão alvo em pixels (300x300)        |
| `IMAGE_WEBP_QUALITY`                | 75      | Qualidade WebP (1-100)                    |

### Build e deploy

```bash
docker build -t descontovivo-api .
docker run -p 8080:8080 --env-file .env descontovivo-api
```

### Comportamento em produção

- Swagger UI: **desligado**
- OpenAPI endpoint: **desligado**
- Health check: **ativo** em `/q/health`

## Integração com Frontend Angular

O contrato completo para integração com `descontovivo-ui` (Angular) está documentado em:

→ [`docs/API_CONTRACTS.md`](docs/API_CONTRACTS.md)

Inclui: endpoints públicos/autenticados/moderação, regras de autorização, tratamento de erros HTTP e variáveis de configuração OIDC para o SPA.

## Checklist de produção

- [ ] Datasource configurado via variáveis de ambiente
- [ ] OIDC/Keycloak configurado e acessível
- [ ] CORS restrito apenas a origens do frontend
- [ ] Swagger UI e OpenAPI desligados (`%prod`)
- [ ] Secrets fora do repositório Git (usar `.env` ou secrets manager)
- [ ] Health check ativo (`/q/health`)
- [ ] Migrations Flyway aplicadas automaticamente no startup
- [ ] Testes passando antes de deploy (`./mvnw clean test`)

### Keycloak — o que precisa bater em produção

| Item                  | Valor esperado                                          |
|-----------------------|---------------------------------------------------------|
| Realm                 | `descontovivo`                                          |
| Issuer                | `https://auth.descontovivo.com/realms/descontovivo`     |
| Client API            | `descontovivo-api` (resource/API, sem fluxo de login)                        |
| Client UI             | `descontovivo-ui` (public, PKCE)                        |
| Algoritmo             | RS256                                                   |
| Access token lifespan | 5 minutos                                               |
| Roles                 | Via `realm_access.roles`                                |
| Web Origins (UI)      | `https://descontovivo.com`                              |
| Redirect URIs (UI)    | `https://descontovivo.com/*`                            |

### Variáveis de ambiente — deploy

```env
# Banco
QUARKUS_DATASOURCE_JDBC_URL=jdbc:postgresql://<host>:5432/descontovivo
QUARKUS_DATASOURCE_USERNAME=<user>
QUARKUS_DATASOURCE_PASSWORD=<password>

# OIDC
OIDC_AUTH_SERVER_URL=https://auth.descontovivo.com/realms/descontovivo
OIDC_CLIENT_ID=descontovivo-api

# CORS
QUARKUS_HTTP_CORS_ORIGINS=https://descontovivo.com

# Cloudflare R2
R2_ENDPOINT=https://<account-id>.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=<access-key>
R2_SECRET_ACCESS_KEY=<secret-key>
R2_BUCKET=descontovivo-promotions
R2_PUBLIC_BASE_URL=https://img.descontovivo.com.br
R2_REGION=auto
```

### Domínios finais

| Serviço   | URL                                |
|-----------|------------------------------------|
| Site/SPA  | `https://descontovivo.com`         |
| API       | `https://api.descontovivo.com`     |
| Auth      | `https://auth.descontovivo.com`    |
| Imagens   | `https://img.descontovivo.com.br`  |
