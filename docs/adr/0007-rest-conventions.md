# ADR-0007: Convenções REST — paginação, erros e versionamento

## Status

Aceita

## Contexto

A API precisa de padrões consistentes para paginação, respostas de erro e versionamento antes de iniciar a implementação.

## Decisão

### Versionamento

- Versão no path: `/api/v1/...`
- Novas versões serão criadas apenas em caso de breaking changes.

### Paginação

- Padrão offset-based com parâmetros `page` (zero-based) e `size`.
- Tamanho padrão: 20. Máximo: 100.
- Resposta inclui `content`, `page`, `size`, `totalElements`, `totalPages`.

### Ordenação

- Parâmetro `sort` com valores pré-definidos por endpoint (ex: `recent`, `popular`, `commented`).
- Default: `recent`.

### Respostas de erro

Formato padrão:

```json
{
  "status": 422,
  "error": "Unprocessable Entity",
  "message": "Descrição legível do erro",
  "violations": [
    { "field": "price", "message": "must be greater than 0" }
  ],
  "timestamp": "2025-01-01T12:00:00Z"
}
```

### Códigos HTTP

- 400: request malformado.
- 401: não autenticado.
- 403: sem permissão.
- 404: recurso não encontrado.
- 409: conflito (ex: duplicidade de promoção no mesmo dia).
- 422: validação de campos.
- 500: erro interno.

### Convenções de verbos

- GET: leitura.
- POST: criação de recursos.
- PUT: operação idempotente (ex: registrar voto).
- PATCH: atualização parcial (ex: ação de moderação).
- DELETE: remoção de recurso ou relação (ex: remover voto).

## Consequências

- Consistência entre todos os endpoints.
- Frontend pode implementar tratamento de erro genérico.
- Paginação padronizada facilita componentes reutilizáveis na UI.
