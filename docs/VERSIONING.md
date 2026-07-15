# Versionamento — DescontoVivo API

## Versão atual

**0.3.3** (patch)

## Convenção

Usamos [SemVer](https://semver.org/) simplificado:

| Tipo | Quando usar | Exemplo |
|------|------------|---------|
| **patch** (0.1.X) | Correção de bug, ajuste pequeno | 0.1.0 → 0.1.1 |
| **minor** (0.X.0) | Nova feature, endpoint, campo | 0.1.0 → 0.2.0 |
| **major** (X.0.0) | Quebra de compatibilidade | 0.2.0 → 1.0.0 |

## Onde atualizar

- `src/main/resources/application.properties` → propriedade `app.version`
- O endpoint `GET /api/v1/version` expõe a versão atual publicamente.

## Processo

1. Toda PR deve avaliar se precisa de bump de versão.
2. O checklist do PR template inclui lembrete de atualização.
3. O footer da UI exibe a versão da API para facilitar validação de deploy.

## Histórico

| Versão | Data       | Descrição |
|--------|------------|-----------|
| 0.3.3  | 2026-07-15 | Limita a deduplicação do import administrativo a promoções equivalentes publicadas há menos de 24 horas |
| 0.3.2  | 2026-07-13 | Corrige a serialização native dos DTOs da inspeção de promoções com registro explícito para reflection |
| 0.3.1  | 2026-07-10 | Permite editar campos de trust signals pelo endpoint de moderação: salesCount, productRating, sellerRating, officialStore e trustSignals |
| 0.3.0  | 2026-07-10 | Trust Signals: novos campos (salesCount, productRating, sellerRating, officialStore, trustSignals) em PromotionSummaryResponse, PromotionDetailResponse e AdminImportItemRequest; migration V12 adiciona colunas; TrustSignalsHelper com parse/serialize JSON e whitelist; exemplo em docs/examples/trust-signals-import-example.json |
| 0.2.1  | 2026-07-09 | Fix admin import with pre-uploaded imageKey bypass |
| 0.2.0  | 2026-07-08 | Add availability and priceSignal fields to AdminImportItemRequest; title normalization server-side |
| 0.1.4  | 2026-07-08 | Fix native image import: decode JPEG/PNG via ImageIO instead of scrimage ImageReaders |
| 0.1.3  | 2026-07-08 | Fix scrimage-webp runtime initialization in native image (defer CWebpHandler/WebpWriter to runtime) |
| 0.1.2  | 2026-07-08 | Fix WebP/cwebp binary in native image (include scrimage resources) |
| 0.1.1  | 2026-07-08 | Fix native image JSON serialization + AWT support |
| 0.1.0  | 2026-07-05 | MVP inicial |
