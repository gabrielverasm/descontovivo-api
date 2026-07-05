# Versionamento — DescontoVivo API

## Versão atual

**0.1.0** (MVP)

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

| Versão | Data | Descrição |
|--------|------|-----------|
| 0.1.0 | 2026-07-05 | MVP inicial |
