# Local Authentication Setup

## Overview

Local Keycloak instance for OIDC/JWT Bearer Token validation during development.  
Endpoints are **not yet protected** — this will be done in a future step.

## How to run

```bash
docker compose up -d postgres keycloak api
```

## URLs

| Service   | URL                                           |
|-----------|-----------------------------------------------|
| API       | http://localhost:8080                          |
| Keycloak  | http://localhost:8082                          |
| Realm     | http://localhost:8082/realms/descontovivo      |

## Keycloak admin console

- URL: http://localhost:8082/admin
- Username: `admin`
- Password: `admin`

> These are local-only credentials, never use them in production.

## Test users (local-only)

| Username    | Password       | Roles                    |
|-------------|----------------|--------------------------|
| user        | user123        | user                     |
| moderator   | moderator123   | user, moderator          |
| admin-user  | admin123       | user, moderator, admin   |

> These credentials exist only in the imported realm for local development.

## How authentication works

1. Frontend (Angular) authenticates via **Authorization Code Flow + PKCE** against Keycloak.
2. Frontend sends requests to the API with `Authorization: Bearer <access_token>`.
3. API validates the JWT signature and claims using Quarkus OIDC (bearer-only / service mode).

## Current state

- Keycloak is running and issuing tokens.
- API is configured to validate tokens (`quarkus.oidc.enabled=true`).
- **No endpoints are protected yet** — `@RolesAllowed` will be added in the next step.
- Legacy `X-Admin-Token` for moderation is still active (temporary).

## Getting a token manually (for testing)

Since `descontovivo-ui` is a public client with PKCE, direct password grant is disabled.  
For quick local testing, you can use the Keycloak token endpoint with a temporary direct-access client, or use the Keycloak admin console to view tokens.

Alternatively, enable `directAccessGrantsEnabled` temporarily on `descontovivo-ui` for local curl tests:

```bash
# Only if directAccessGrantsEnabled is temporarily enabled on the client:
curl -s -X POST http://localhost:8082/realms/descontovivo/protocol/openid-connect/token \
  -d "client_id=descontovivo-ui" \
  -d "username=user" \
  -d "password=user123" \
  -d "grant_type=password" | jq .access_token
```
