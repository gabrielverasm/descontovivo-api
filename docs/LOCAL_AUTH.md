# Local Authentication Setup

## Overview

Local Keycloak instance for OIDC/JWT Bearer Token validation during development.  
Protected endpoints require a valid `Authorization: Bearer <access_token>` header.

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

## Endpoint security

### Public (no token required)

- `GET /api/v1/system/info`
- `GET /api/v1/stores`
- `GET /api/v1/stores/{slug}`
- `GET /api/v1/promotions`
- `GET /api/v1/promotions/{slug}`
- `GET /api/v1/promotions/{slug}/comments`

### Authenticated (valid token + email_verified=true)

- `POST /api/v1/promotions`
- `PUT /api/v1/promotions/{slug}/vote`
- `DELETE /api/v1/promotions/{slug}/vote`
- `POST /api/v1/promotions/{slug}/comments`
- `POST /api/v1/comments/{id}/replies`

### Moderator/Admin (role: moderator or admin)

- `GET /api/v1/moderation/promotions`
- `PATCH /api/v1/moderation/promotions/{id}`
- `PATCH /api/v1/moderation/comments/{id}`

## Important changes

- **X-Admin-Token has been removed.** Moderation now uses OIDC roles (`moderator`, `admin`).
- **clientId is no longer sent by the frontend.** The user identity comes from the JWT `sub` claim.
- The frontend must use the `sub` claim implicitly via the token — do not send user ID in the request body.

## How authentication works

1. Frontend (Angular) authenticates via **Authorization Code Flow + PKCE** against Keycloak.
2. Frontend sends requests to the API with `Authorization: Bearer <access_token>`.
3. API validates the JWT signature and claims using Quarkus OIDC (bearer-only / service mode).
4. User identity (`sub`) is extracted from the token — no need to pass it in request bodies.

## Getting a token manually (for testing)

The realm JSON enables `directAccessGrantsEnabled=false` on `descontovivo-ui` for security.  
For quick local curl testing, you can temporarily enable it in the Keycloak admin console, or create a separate test client.

Example (if direct access is temporarily enabled):

```bash
TOKEN=$(curl -s -X POST http://localhost:8082/realms/descontovivo/protocol/openid-connect/token \
  -d "client_id=descontovivo-ui" \
  -d "username=moderator" \
  -d "password=moderator123" \
  -d "grant_type=password" | jq -r .access_token)

curl -H "Authorization: Bearer $TOKEN" http://localhost:8080/api/v1/moderation/promotions
```
