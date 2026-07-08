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
| Deploy atual | Manual (rollback) | ✅ Produção (GHCR + VPS, automático) |

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

## 10. CI/CD — Deploy native automático após merge

### Fluxo principal

```
PR merge → master → workflow "Deploy API Native"
                         │
                         ├─ Job: native-build
                         │    ├─ Roda testes JVM (./mvnw clean verify)
                         │    ├─ Compila native (Mandrel container-build)
                         │    ├─ Push GHCR: native-<sha> + native-manual
                         │    └─ Summary no GitHub
                         │
                         └─ Job: deploy-production (needs: native-build)
                              ├─ ⏸️ Aguarda aprovação no environment "production"
                              ├─ Copia docker-compose.prod.yml para VPS
                              ├─ Gera .env com secrets + IMAGE_TAG=native-<sha>
                              ├─ Pull + up container
                              ├─ Health check (https://api.descontovivo.com/q/health)
                              └─ Rollback automático se health falhar
```

### Acionamento

| Evento | Comportamento |
|--------|--------------|
| Push na `master` (merge de PR) | Dispara automaticamente |
| `workflow_dispatch` | Execução manual (Actions → Deploy API Native → Run workflow) |

### Tags publicadas

| Tag | Descrição |
|-----|-----------|
| `ghcr.io/gabrielverasm/descontovivo-api:native-<sha>` | Imutável, vinculada ao commit |
| `ghcr.io/gabrielverasm/descontovivo-api:native-manual` | Sobrescrita a cada execução |

> A tag `latest` **não é afetada** — permanece apontando para a imagem JVM (usada pelo workflow de rollback).

### Aprovação do deploy

O job `deploy-production` usa `environment: production` com proteção de aprovação configurada no GitHub.

1. O build native roda e publica a imagem no GHCR.
2. O deploy **para e aguarda aprovação** no GitHub.
3. Após aprovação manual, o deploy aplica a imagem native na VPS.
4. Se o health check falhar, faz rollback automático usando backup do `.env`.

### Segurança

- Secrets são passados via `env` do step SSH — nunca impressos em logs.
- `.env` na VPS tem `chmod 600`.
- Backup criado antes de sobrescrever (`.env.backup.<timestamp>`).
- Health check com rollback automático protege contra deploy quebrado.
- `timeout-minutes: 45` (build) e `timeout-minutes: 10` (deploy).

### Limitações

- O runner `ubuntu-latest` tem 7 GB RAM. O build native consome ~6 GB — funciona, mas sem muita folga.
- Se falhar por OOM, a solução futura é usar runner maior ou self-hosted.

## 11. Rollback JVM manual

O workflow **Deploy API JVM Manual** (`.github/workflows/deploy-api.yml`) existe exclusivamente para rollback ou deploy JVM sob demanda.

### Quando usar

- A imagem native apresentou problema em produção.
- Precisa voltar para JVM enquanto investiga.
- Precisa fazer deploy de uma versão JVM específica.

### Acionamento

Actions → **Deploy API JVM Manual** → **Run workflow**.

> ⚠️ **Não roda automaticamente em push na master.** Apenas execução manual.

### O que faz

1. Roda testes JVM (`./mvnw clean verify`).
2. Builda imagem JVM e publica no GHCR (tags: `latest` + `<sha>`).
3. Aguarda aprovação no environment `production`.
4. Faz deploy JVM na VPS com `IMAGE_TAG=<sha>`.
5. Health check com retry.

### Rollback rápido na VPS (sem CI)

Se precisar reverter imediatamente sem esperar CI:

```bash
ssh <vps>
cd /opt/descontovivo/api
# O .env.backup.* mais recente tem a IMAGE_TAG anterior
cp .env.backup.<timestamp> .env
docker compose -f docker-compose.prod.yml pull
docker compose -f docker-compose.prod.yml up -d --remove-orphans
curl -f https://api.descontovivo.com/q/health
```

## 12. Resumo dos workflows

| Workflow | Arquivo | Trigger | Finalidade |
|----------|---------|---------|------------|
| Deploy API Native | `native-image.yml` | push master + manual | Build native + deploy produção (fluxo principal) |
| Deploy API JVM Manual | `deploy-api.yml` | manual | Rollback JVM / deploy JVM sob demanda |

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

### scrimage-webp binary resources

Em `application.properties`:

```properties
quarkus.native.resources.includes=dist_webp_binaries/**
```

O GraalVM native-image **não inclui resources de JARs de dependência** por padrão. A lib scrimage-webp extrai o binário `cwebp` (e `dwebp`) do classpath via `Class.getResourceAsStream()` e escreve em `/tmp` para executar. Sem essa configuração, `getResourceAsStream()` retorna `null` e o binário extraído é inválido/vazio, causando:

```
Cannot run program "/tmp/cwebp...binary": Exec failed, error: 2 (No such file or directory)
```

O binário bundled é **statically linked** — não precisa de libs compartilhadas do sistema. Só precisa ser extraído corretamente para `/tmp` com permissão de execução.

### scrimage-webp runtime initialization (v0.1.3)

Incluir os resources (`dist_webp_binaries/**`) **não é suficiente**. As classes do scrimage-webp (`CWebpHandler`, `DWebpHandler`, `WebpHandler`, `WebpWriter`) possuem static initializers que:

1. Localizam o binário `cwebp`/`dwebp` no classpath via `getResourceAsStream()`.
2. Extraem o binário para um arquivo temporário em `/tmp` (e.g., `/tmp/cwebp5425043227069466810binary`).
3. Armazenam o **path absoluto** do arquivo temporário em campos estáticos.

Se essas classes são inicializadas em **build-time** (comportamento padrão do GraalVM), o path temporário criado **durante o build** fica gravado ("baked") no binário native. Em runtime, esse arquivo não existe no container, causando:

```
Cannot run program "/tmp/cwebp5425043227069466810binary": Exec failed, error: 2 (No such file or directory)
```

**Correção:** forçar inicialização em runtime para que a extração do binário aconteça no container:

```properties
quarkus.native.additional-build-args=--initialize-at-run-time=org.apache.http.impl.auth.NTLMEngineImpl,\
  --initialize-at-run-time=com.sksamuel.scrimage.webp.CWebpHandler,\
  --initialize-at-run-time=com.sksamuel.scrimage.webp.DWebpHandler,\
  --initialize-at-run-time=com.sksamuel.scrimage.webp.WebpHandler,\
  --initialize-at-run-time=com.sksamuel.scrimage.webp.WebpWriter
```

> **Nota:** Usamos argumentos `--initialize-at-run-time` separados por vírgula (separador de argumentos do Quarkus) ao invés de uma lista dentro de um único argumento, para evitar problemas de escape de vírgula no parser do Quarkus.

## Arquivos relacionados

- `src/main/docker/Dockerfile.native` — Dockerfile para imagem native
- `src/main/docker/Dockerfile.native.dockerignore` — context filter para build Docker native
- `docker-compose.native.yml` — compose override local/dev (não usar sozinho nem em produção)
- `src/main/resources/application.properties` — config native (propriedade `quarkus.native.additional-build-args`)
