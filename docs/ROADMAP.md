# Roadmap — DescontoVivo API

## MVP atual (implementado)

- [x] Feed público de promoções com paginação, busca e filtros.
- [x] Publicação autenticada com moderação obrigatória.
- [x] Moderação por roles (`moderator`, `admin`): aprovar, rejeitar, remover, editar.
- [x] Votação (like/dislike) por usuário autenticado.
- [x] Comentários e respostas em promoções publicadas.
- [x] Moderação de comentários (soft delete).
- [x] Upload de imagem via presigned URL + promoção de temp para R2.
- [x] Troca de imagem na edição/moderação via `imageKey` temporário.
- [x] Import admin por JSON com download/processamento de imagem para R2.
- [x] Backfill administrativo de imagens externas para R2.
- [x] SSRF protection no download de imagens.
- [x] Segurança JWT Bearer + Keycloak/OIDC.
- [x] Testes automatizados (QuarkusTest + Testcontainers).
- [x] Health check e OpenAPI em dev.
- [x] Flyway migrations automáticas.
- [x] Deploy em container Docker.

## Próximas etapas

- [ ] Worker/crawler automático de ofertas (buscar promoções de lojas).
- [ ] Job de limpeza de imagens órfãs no R2 (`temp/promotions/` com mais de 7 dias).
- [ ] Sitemap dinâmico de promoções para SEO.
- [ ] Categorias com endpoint próprio.
- [ ] Ordenação avançada no feed (`popular`, `commented`).
- [ ] Notificações (e-mail ou push) para autores quando promoção é aprovada/rejeitada.
- [ ] Dashboard administrativo com métricas.
- [ ] Integração com programas de afiliados.
- [ ] Rate limiting por IP/usuário.
- [ ] Cache HTTP em endpoints públicos.
- [ ] Monitoramento e observabilidade (métricas, tracing).

## Fora do escopo

- SSR/prerender (responsabilidade do frontend).
- Autenticação própria (delegada ao Keycloak).
- Persistência de senha de usuário.
