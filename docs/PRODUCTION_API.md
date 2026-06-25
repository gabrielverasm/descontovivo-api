# Produção — API DescontoVivo

## Visão geral

A API roda como container Docker na VPS, atrás de Caddy (reverse proxy com TLS automático).

O fluxo de deploy é:

```
push master → GitHub Actions (test → build → push GHCR → scp compose → SSH deploy) → health check
```

A cada deploy o workflow:
1. Copia `docker-compose.prod.yml` para a VPS
2. Gera `/opt/descontovivo/api/.env` com chmod 600 (a partir dos Environment Secrets)
3. Faz pull da imagem pelo SHA do commit
4. Reinicia somente o container da API

## Domínios

| Serviço | URL |
|---------|-----|
| API | `https://api.descontovivo.com` |
| SPA | `https://descontovivo.com` |
| Auth | `https://auth.descontovivo.com` |

## Variáveis de ambiente (geradas pelo workflow)

Arquivo: `/opt/descontovivo/api/.env` — gerado automaticamente, nunca versionado.

| Variável | Descrição |
|----------|-----------|
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC URL do PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | Usuário do banco |
| `QUARKUS_DATASOURCE_PASSWORD` | Senha do banco |
| `OIDC_AUTH_SERVER_URL` | `https://auth.descontovivo.com/realms/descontovivo` |
| `OIDC_CLIENT_ID` | `descontovivo-api` |
| `QUARKUS_HTTP_CORS_ORIGINS` | `https://descontovivo.com` |
| `IMAGE_TAG` | SHA do commit deployado |

## GitHub Secrets (Environment: `production`)

Configurar em: **Settings → Environments → production → Environment secrets**

| Secret | Descrição |
|--------|-----------|
| `VPS_HOST` | IP ou hostname da VPS |
| `VPS_PORT` | Porta SSH (ex: 22) |
| `VPS_USER` | Usuário SSH (`gabriel`) |
| `VPS_SSH_KEY` | Chave privada SSH (ed25519 recomendado) |
| `API_DB_URL` | JDBC URL do PostgreSQL (ver nota abaixo) |
| `API_DB_USER` | Usuário do banco |
| `API_DB_PASSWORD` | Senha do banco |
| `API_OIDC_AUTH_SERVER_URL` | `https://auth.descontovivo.com/realms/descontovivo` |
| `API_OIDC_CLIENT_ID` | `descontovivo-api` |
| `API_CORS_ORIGINS` | `https://descontovivo.com` |

> `GITHUB_TOKEN` é usado automaticamente para push no GHCR.

### Nota sobre `API_DB_URL`

Dentro do container da API, `localhost` aponta para o próprio container, não para a VPS. O valor correto depende de onde o PostgreSQL roda:

| Cenário | Exemplo de JDBC URL |
|---------|--------------------|
| Banco externo/gerenciado | `jdbc:postgresql://<host-externo>:5432/descontovivo` |
| Banco em outro container na mesma rede Docker | `jdbc:postgresql://<nome-do-servico>:5432/descontovivo` |
| Banco instalado direto na VPS (host) | `jdbc:postgresql://host.docker.internal:5432/descontovivo` — requer `extra_hosts: ["host.docker.internal:host-gateway"]` no compose, ou usar o IP da interface `docker0` |

## Estratégia de imagem (GHCR)

- Registry: `ghcr.io/gabrielverasm/descontovivo-api`
- Tags: `latest` (branch master) + SHA do commit
- O compose usa `${IMAGE_TAG:-latest}` — o SHA é definido no `.env` gerado pelo workflow
- Isso permite rastrear exatamente qual commit está rodando em produção

### Login GHCR na VPS (primeira vez)

A VPS precisa autenticar no GHCR para pull da imagem. Criar um PAT (classic) com escopo `read:packages`:

```bash
echo "<PAT>" | docker login ghcr.io -u gabrielverasm --password-stdin
```

> Fazer apenas uma vez. As credenciais ficam em `~/.docker/config.json`.

## Preparação manual da VPS

Estes passos são necessários apenas **uma vez**, antes do primeiro deploy.

> **Pré-requisito:** o usuário `gabriel` deve conseguir executar `docker ps` e `docker compose` sem `sudo` (pertencer ao grupo `docker`). O login no GHCR também deve ser feito como `gabriel`, não como root — caso contrário o workflow não terá acesso às credenciais de pull.

### 1. Criar rede Docker externa

```bash
docker network create descontovivo-public
```

### 2. Conectar Caddy à rede (se ainda não estiver)

No compose do Caddy/auth, adicionar a rede `descontovivo-public` como external e conectar o serviço Caddy a ela. Depois:

```bash
cd /opt/descontovivo/auth
docker compose up -d
```

Ou conectar sem reiniciar:

```bash
docker network connect descontovivo-public <caddy-container-name>
```

### 3. Criar diretório da API

```bash
sudo mkdir -p /opt/descontovivo/api
sudo chown gabriel:gabriel /opt/descontovivo/api
```

### 4. Login no GHCR

```bash
echo "<PAT>" | docker login ghcr.io -u gabrielverasm --password-stdin
```

> O `docker-compose.prod.yml` e o `.env` são enviados automaticamente pelo workflow. Não é necessário copiá-los manualmente.

### Deploy manual (sem GitHub Actions)

Caso precise subir a API sem o workflow (ex: primeiro boot ou emergência):

```bash
cd /opt/descontovivo/api
# Criar .env manualmente com as variáveis listadas acima
nano .env
chmod 600 .env
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
```

## Caddy — Adicionar rota para API

Editar o Caddyfile (provavelmente em `/opt/descontovivo/auth/Caddyfile` ou equivalente):

```caddyfile
api.descontovivo.com {
    reverse_proxy descontovivo-api:8080
}
```

Recarregar Caddy:

```bash
docker exec <caddy-container-name> caddy reload --config /etc/caddy/Caddyfile
```

> O nome `descontovivo-api` resolve via rede Docker `descontovivo-public`.

## DNS (Cloudflare)

Criar registro depois que a VPS e Caddy estiverem prontos:

| Campo | Valor |
|-------|-------|
| Type | `A` |
| Name | `api` |
| IPv4 | IP da VPS |
| Proxy | DNS only (cinza) inicialmente |
| TTL | Auto |

Depois que HTTPS estiver funcionando via Caddy, avaliar ativar proxy (laranja).

## Health check

```bash
curl -f https://api.descontovivo.com/q/health
```

Esperado: HTTP 200 com status `UP`.

## Troubleshooting

| Problema | Verificação |
|----------|-------------|
| API não inicia | `docker logs descontovivo-api` |
| Health check falha | Verificar variáveis no `.env`, conectividade com DB e Keycloak |
| 502 no Caddy | Verificar se API e Caddy estão na mesma rede Docker |
| Pull falha | Verificar login GHCR na VPS |
| Flyway falha | Verificar `QUARKUS_DATASOURCE_*` e acessibilidade do PostgreSQL |
| Qual commit está rodando? | `docker exec descontovivo-api printenv IMAGE_TAG` ou `grep IMAGE_TAG /opt/descontovivo/api/.env` |

## Checklist — Primeiro deploy

- [ ] Rede `descontovivo-public` criada
- [ ] Caddy conectado à rede
- [ ] Caddyfile com rota `api.descontovivo.com`
- [ ] Diretório `/opt/descontovivo/api` criado com owner `gabriel`
- [ ] Login GHCR feito como usuário `gabriel` (não root)
- [ ] DNS `api.descontovivo.com` apontando para VPS
- [ ] GitHub Environment `production` criado
- [ ] Todos os secrets configurados no environment
- [ ] Push na `master` dispara workflow
- [ ] Health check retorna 200
