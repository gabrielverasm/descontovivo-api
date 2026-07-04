# Native Image — DescontoVivo API

## 1. Objetivo

Gerar e executar a API como binário nativo (GraalVM/Mandrel), obtendo startup sub-segundo e menor consumo de memória, mantendo compatibilidade total com o modo JVM.

## 2. JVM vs Native neste projeto

| Aspecto | JVM | Native |
|---------|-----|--------|
| Startup | ~4.3s | ~0.13s (31x mais rápido) |
| RSS (idle + requests) | ~284 MB | ~124 MB (56% menos) |
| Build time | ~17s | ~4–5 min |
| Build RAM | ~2 GB | ~6 GB |
| Binário | `target/quarkus-app/quarkus-run.jar` | `target/*-runner` (142 MB ELF) |
| Docker image | ~380 MB (Temurin JRE) | ~404 MB (UBI9-minimal + binary) |
| Debug/profiling | Completo (JMX, JFR, etc.) | Limitado |
| Reflection | Transparente | Tratado pelo Quarkus em build-time |
| Deploy atual | ✅ Produção (GHCR + VPS) | 🧪 Pronto para canary/staging |

## 3. Build native local

### Pré-requisitos

- Docker rodando (o build é feito dentro de container Mandrel)
- ~6 GB RAM disponível para o container de build
- ~10 min na primeira vez (pull da imagem builder)

### Comando

```bash
./mvnw package -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true -DskipTests
```

O Quarkus usa automaticamente a imagem `quay.io/quarkus/ubi9-quarkus-mandrel-builder-image:jdk-25`.

### Verificar artefato

```bash
ls -lh target/*-runner
file target/*-runner
# ELF 64-bit LSB executable, x86-64, dynamically linked
```

## 4. Executar native local

### Requisitos

- PostgreSQL rodando (ex: `docker compose up -d postgres`)
- Variáveis de ambiente configuradas

### Comando

```bash
R2_ENDPOINT="http://localhost:9000" \
R2_ACCESS_KEY_ID="dev-access-key" \
R2_SECRET_ACCESS_KEY="dev-secret-key" \
OIDC_AUTH_SERVER_URL="http://localhost:8082/realms/descontovivo" \
OIDC_CLIENT_ID="descontovivo-api" \
QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://localhost:5432/descontovivo" \
QUARKUS_DATASOURCE_USERNAME="descontovivo" \
QUARKUS_DATASOURCE_PASSWORD="descontovivo" \
./target/descontovivo-api-0.1.0-SNAPSHOT-runner
```

OIDC pode emitir warning se Keycloak não estiver rodando — a API sobe normalmente e tenta reconectar no primeiro request autenticado.

## 5. Docker native (local/dev)

### Build da imagem

```bash
# 1. Compilar binário native (se ainda não fez)
./mvnw package -Dquarkus.native.enabled=true \
  -Dquarkus.native.container-build=true -DskipTests

# 2. Construir imagem Docker
docker build -f src/main/docker/Dockerfile.native -t descontovivo-api:native .
```

### Executar container (standalone)

```bash
docker run --rm -p 8080:8080 \
  -e QUARKUS_DATASOURCE_JDBC_URL="jdbc:postgresql://host.docker.internal:5432/descontovivo" \
  -e QUARKUS_DATASOURCE_USERNAME="descontovivo" \
  -e QUARKUS_DATASOURCE_PASSWORD="descontovivo" \
  -e OIDC_AUTH_SERVER_URL="http://host.docker.internal:8082/realms/descontovivo" \
  -e OIDC_CLIENT_ID="descontovivo-api" \
  -e R2_ENDPOINT="http://host.docker.internal:9000" \
  -e R2_ACCESS_KEY_ID="dev-access-key" \
  -e R2_SECRET_ACCESS_KEY="dev-secret-key" \
  descontovivo-api:native
```

### Com Docker Compose (dev completo)

O `docker-compose.native.yml` é um **override local/dev** do `docker-compose.yml` base.
Ele substitui apenas o serviço `api` para usar a imagem native em vez da JVM.

**Não deve ser usado sozinho** (depende de `postgres` e `keycloak` definidos no compose base).
**Não é compose de produção.**

```bash
docker compose -f docker-compose.yml -f docker-compose.native.yml up --build
```

### Healthcheck

A imagem runtime (`ubi9/ubi-minimal:9.6`) **não contém curl** nem ferramentas de rede.
Por isso, o `docker-compose.native.yml` **não inclui healthcheck interno**.

O smoke test deve ser feito **de fora do container**:

```bash
curl -f http://localhost:8080/q/health
```

## 6. Testar health e SSE

```bash
# Health check (de fora do container)
curl -i http://localhost:8080/q/health

# Promotions (público)
curl -i "http://localhost:8080/api/v1/promotions?page=0&size=1"

# SSE stream público (Ctrl+C para sair)
curl -N http://localhost:8080/api/v1/events/public/stream

# Admin stream (deve retornar 401 sem token)
curl -i http://localhost:8080/api/v1/events/admin/stream
```

## 7. Variáveis obrigatórias

Mesmas variáveis do modo JVM — nenhuma variável nova para native.

| Variável | Descrição |
|----------|-----------|
| `QUARKUS_DATASOURCE_JDBC_URL` | JDBC URL do PostgreSQL |
| `QUARKUS_DATASOURCE_USERNAME` | Usuário do banco |
| `QUARKUS_DATASOURCE_PASSWORD` | Senha do banco |
| `OIDC_AUTH_SERVER_URL` | URL do realm Keycloak |
| `OIDC_CLIENT_ID` | Client ID OIDC |
| `QUARKUS_HTTP_CORS_ORIGINS` | Origens CORS |
| `R2_ENDPOINT` | Endpoint S3/R2 |
| `R2_ACCESS_KEY_ID` | Access key |
| `R2_SECRET_ACCESS_KEY` | Secret key |
| `R2_BUCKET` | Bucket name |
| `R2_PUBLIC_BASE_URL` | URL pública imagens |

## 8. Limitações conhecidas

1. **Build pesado:** ~6 GB RAM, ~5 min. GitHub Actions ubuntu-latest tem 7 GB — funciona, mas sem muita folga.

2. **Debug limitado:** Sem JMX, JFR, ou attach de debugger em native. Para troubleshooting profundo, usar JVM.

3. **SSE serialização:** Os records do módulo `notification.dto` (`PublicPromotionSnapshot`, `ModerationPromotionSnapshot`, `AdminDataRequestSnapshot`) não são retornados diretamente por endpoints JAX-RS — são serializados internamente pelo `NotificationPayloadFactory` via `ObjectMapper`. O Quarkus não registra automaticamente reflection para esses tipos, portanto requerem `@RegisterForReflection`. Sem a anotação, Jackson serializa os records como `{}` em native (sem lançar exceção).

4. **Imagem Docker grande:** 404 MB (UBI9-minimal + binário + AWT libs). Poderia ser menor com imagem distroless estática se não precisássemos de AWT/scrimage, mas por enquanto é aceitável.

5. **AWT libs:** O scrimage usa Java AWT para processamento de imagens. O native-image gera automaticamente `libawt.so`, `libawt_headless.so`, `liblcms.so` etc. que precisam estar no mesmo diretório do binário em runtime.

6. **scrimage-webp cwebp:** O binário `cwebp` é extraído do JAR para `/tmp` em runtime. Funciona porque é statically linked. O diretório `/tmp` precisa ser writable no container.

7. **Sem healthcheck interno:** A imagem runtime não contém curl/wget. Healthcheck deve ser feito externamente (load balancer, Docker host, ou orquestrador).

## 9. Comparação JVM vs Native

Medido localmente (WSL2, 12 CPUs, 15.5 GB RAM, PostgreSQL local):

| Métrica | JVM | Native | Ganho |
|---------|-----|--------|-------|
| Startup (com Flyway validate) | 4.268s | 0.135s | 31.6x |
| RSS após requests | ~284 MB | ~124 MB | -56% |
| Health `/q/health` | ✅ 200 | ✅ 200 | — |
| GET `/api/v1/promotions` | ✅ 200 | ✅ 200 | — |
| SSE `/api/v1/events/public/stream` | ✅ | ✅ | — |
| Admin stream sem token | ✅ 401 | ✅ 401 | — |

## 10. Plano de deploy em produção

### Situação atual

- **Produção continua usando JVM** (`docker-compose.prod.yml` / imagem JVM no GHCR).
- **Native em produção é canary manual** — não será ativado automaticamente.
- Nenhuma automação de deploy native existe por enquanto.
- A decisão de migrar para native em produção será tomada apenas após validação canary completa.

### Estratégia: canary side-by-side (manual)

```
┌─────────────────────────────────────────┐
│ VPS                                     │
│                                         │
│  [JVM container :8080] ← tráfego real  │
│  [Native container :8085] ← smoke test │
│                                         │
└─────────────────────────────────────────┘
```

### Passos

1. **Build** imagem native localmente ou no CI:
   ```bash
   ./mvnw package -Dquarkus.native.enabled=true \
     -Dquarkus.native.container-build=true -DskipTests
   docker build -f src/main/docker/Dockerfile.native \
     -t ghcr.io/gabrielverasm/descontovivo-api:native-$(git rev-parse --short HEAD) .
   docker push ghcr.io/gabrielverasm/descontovivo-api:native-$(git rev-parse --short HEAD)
   ```

2. **Deploy canary** na VPS (porta alternativa):
   ```bash
   docker run -d --name descontovivo-api-native \
     --env-file .env \
     --network descontovivo-public \
     -p 8085:8080 \
     ghcr.io/gabrielverasm/descontovivo-api:native-<sha>
   ```

3. **Smoke tests** no servidor (de fora do container):
   ```bash
   curl -f http://localhost:8085/q/health
   curl "http://localhost:8085/api/v1/promotions?page=0&size=1"
   curl -N --max-time 10 http://localhost:8085/api/v1/events/public/stream
   ```

4. **Swap** (se tudo OK):
   ```bash
   docker stop descontovivo-api        # para JVM
   docker stop descontovivo-api-native
   # Reiniciar native na porta 8080
   docker run -d --name descontovivo-api \
     --env-file .env \
     --network descontovivo-public \
     -p 8080:8080 \
     ghcr.io/gabrielverasm/descontovivo-api:native-<sha>
   ```

5. **Confirmar:**
   ```bash
   curl -f https://api.descontovivo.com/q/health
   ```

### Rollback para JVM

```bash
docker stop descontovivo-api
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d
# Confirmar
curl -f https://api.descontovivo.com/q/health
```

Rollback é instantâneo — a imagem JVM anterior está no registry.

## 11. O que falta antes de produção

- [ ] Testar com Keycloak real (validar JWT em native com RS256)
- [ ] Testar upload de imagem + processamento WebP em native com R2 real
- [ ] Validar SSE com múltiplos clientes simultâneos em native
- [ ] Investigar primeiro tick SSE vazio observado em testes locais
- [ ] Monitorar memória/CPU em produção por ao menos 24h em canary
- [x] Decidir se CI vai buildar native automaticamente ou se será manual → **manual via workflow_dispatch** (seção 12)
- [ ] Considerar multi-stage Dockerfile (builder + runtime) se quiser CI self-contained

## 12. GitHub Actions — build manual

O workflow **Native Image Build** (`.github/workflows/native-image.yml`) permite gerar e publicar a imagem native no GHCR sob demanda, sem afetar produção.

### Acionamento

Actions → **Native Image Build** → **Run workflow** (branch desejada).

Não roda automaticamente em push nem pull request.

### O que faz

1. Roda testes JVM (`./mvnw clean verify`).
2. Compila native com container-build (Mandrel).
3. Verifica artefato gerado (`target/*-runner`).
4. Builda e publica imagem Docker native no GHCR.

### Tags publicadas

| Tag | Descrição |
|-----|-----------|
| `ghcr.io/gabrielverasm/descontovivo-api:native-<sha>` | Imutável, vinculada ao commit |
| `ghcr.io/gabrielverasm/descontovivo-api:native-manual` | Sobrescrita a cada execução manual |

> A tag `latest` **não é afetada** — continua apontando para a imagem JVM de produção.

### O que NÃO faz

- Não faz deploy na VPS.
- Não altera produção.
- Não usa secrets de produção (apenas `GITHUB_TOKEN` para push no GHCR).
- Não substitui o workflow JVM (`deploy-api.yml`).

### Limitações

- O runner `ubuntu-latest` tem 7 GB RAM. O build native consome ~6 GB — funciona, mas sem muita folga.
- Se falhar por OOM, a solução futura é usar runner maior ou self-hosted.
- `timeout-minutes: 45` para evitar builds travados.

### Canary manual após build

Após a execução do workflow, a imagem pode ser usada para canary manual na VPS (ver seção 10).

## Configuração nativa no projeto

### HTTP Client explícito para AWS SDK S3

Em native image, o AWS SDK **não consegue descobrir automaticamente** a implementação HTTP via service loader/provider chain. O erro é:

```
SdkClientException: Unable to load an HTTP implementation from any provider in the chain.
```

**Correção:** o `S3ClientProducer` usa `UrlConnectionHttpClient` explicitamente:

```java
S3Client.builder()
    .httpClient(UrlConnectionHttpClient.create())
    // ...
    .build();
```

A dependência `software.amazon.awssdk:url-connection-client` é declarada no `pom.xml`.

Isso garante que o S3Client funciona tanto em JVM quanto em native sem depender de provider discovery.

### NTLMEngineImpl deferred initialization

Em `application.properties`:

```properties
quarkus.native.additional-build-args=--initialize-at-run-time=org.apache.http.impl.auth.NTLMEngineImpl
```

Isso resolve o `SecureRandom` no class initializer do Apache HTTP Client (dependência transitiva do AWS SDK S3).

## Arquivos relacionados

- `src/main/docker/Dockerfile.native` — Dockerfile para imagem native
- `src/main/docker/Dockerfile.native.dockerignore` — context filter para build Docker native
- `docker-compose.native.yml` — compose override local/dev (não usar sozinho nem em produção)
- `src/main/resources/application.properties` — config native (propriedade `quarkus.native.additional-build-args`)
