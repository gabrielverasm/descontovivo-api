# Keycloak Local — Guia de Desenvolvimento

Configuração local do Keycloak para testar a integração entre `descontovivo-api` e `descontovivo-ui`.

## Subir o Keycloak

```bash
docker compose -f docker-compose.keycloak.yml up -d
```

## Logs

```bash
docker compose -f docker-compose.keycloak.yml logs -f keycloak
```

## Parar

```bash
docker compose -f docker-compose.keycloak.yml down
```

## URLs

| Recurso            | URL                                                                  |
|--------------------|----------------------------------------------------------------------|
| Admin Console      | http://localhost:8082                                                 |
| Login admin        | `admin` / `admin`                                                    |
| Realm              | http://localhost:8082/realms/descontovivo                             |
| OIDC Discovery     | http://localhost:8082/realms/descontovivo/.well-known/openid-configuration |

## Usuários de Teste

| Username    | Email                          | Senha          | Roles              |
|-------------|--------------------------------|----------------|--------------------|
| user        | user@descontovivo.local        | user123        | user               |
| moderator   | moderator@descontovivo.local   | moderator123   | user, moderator    |
| admin-user  | admin@descontovivo.local       | admin123       | user, admin        |

Todos possuem `emailVerified: true`, `firstName`, `lastName` e `requiredActions: []` (sem tela de atualização de perfil no primeiro login).

## Clients Configurados

| Client ID         | Tipo           | Uso                          |
|-------------------|----------------|------------------------------|
| descontovivo-ui   | Public (PKCE)  | SPA Angular em localhost:4200 |
| descontovivo-api  | Bearer-only    | Resource server da API        |

## Checklist — Testar com Frontend

1. Keycloak rodando em `http://localhost:8082`
2. API rodando em `http://localhost:8080` (`./mvnw quarkus:dev`)
3. UI rodando em `http://localhost:4200` (`ng serve`)
4. Abrir `http://localhost:4200/login`
5. Clicar em "Entrar" → redireciona para Keycloak
6. Autenticar com um usuário de teste (ex: `user` / `user123`)
7. Verificar se volta para o frontend com sessão ativa
8. Inspecionar Network: `GET /api/v1/account/me` deve enviar header `Authorization: Bearer <token>`
9. Resposta deve conter `roles` via `realm_access.roles`

## Troubleshooting

### Porta 8082 ocupada

```bash
lsof -i :8082
# ou mude a porta no docker-compose.keycloak.yml
```

### Issuer inválido / token rejeitado pela API

Verifique que `application.properties` tem:

```properties
quarkus.oidc.auth-server-url=http://localhost:8082/realms/descontovivo
```

O issuer no token deve ser exatamente `http://localhost:8082/realms/descontovivo`.

### Redirect URI inválida no login

O client `descontovivo-ui` precisa ter `http://localhost:4200/*` em redirect URIs. Já está configurado no realm import.

### Erro de CORS em `GET /api/v1/account/me`

Se o preflight (`OPTIONS`) passa mas o `GET` com Bearer token falha com erro de CORS no navegador, verifique a configuração CORS da API em `application.properties`:

```properties
%dev.quarkus.http.cors=true
%dev.quarkus.http.cors.origins=http://localhost:4200
%dev.quarkus.http.cors.methods=GET,POST,PUT,PATCH,DELETE,OPTIONS
%dev.quarkus.http.cors.headers=accept,authorization,content-type,x-requested-with
%dev.quarkus.http.cors.exposed-headers=location
```

Pontos críticos:

- `authorization` deve estar em `headers` — sem isso, o navegador bloqueia requests com Bearer token.
- Definir apenas `origins` não é suficiente; `methods` e `headers` precisam ser explícitos.
- CORS headers devem ser retornados em todas as respostas (200, 401, 403, 500), não apenas no OPTIONS.

### Erro de CORS / Web Origins no Keycloak

O client `descontovivo-ui` precisa ter `http://localhost:4200` em Web Origins. Já está configurado no realm import.

### Usuário sem emailVerified

A API retorna 403 para ações autenticadas quando `emailVerified=false`. Os usuários de teste já vêm com `emailVerified: true`.

### Realm não importou (container com dados antigos)

O import do realm só ocorre na primeira inicialização do container. Se você alterou o `descontovivo-realm.json` e precisa que as mudanças sejam aplicadas, é necessário recriar o container:

```bash
docker compose -f docker-compose.keycloak.yml down -v
docker ps -a --filter "name=descontovivo-keycloak"
docker rm -f descontovivo-keycloak   # se ainda existir
docker compose -f docker-compose.keycloak.yml up -d
```

> **Aviso:** Não use `--remove-orphans` porque pode afetar containers da API/Postgres que estejam em execução via outro compose file.
