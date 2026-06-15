# ADR-0005: Separar status de moderação e disponibilidade da oferta

## Status

Aceita

## Contexto

Uma promoção publicada deve continuar visível no feed/histórico mesmo que a oferta acabe.

Ao mesmo tempo, promoções rejeitadas ou removidas não devem aparecer publicamente.

## Decisão

Usaremos dois campos separados:

```txt
PromotionStatus:
- PENDING_REVIEW
- PUBLISHED
- REJECTED
- REMOVED

AvailabilityStatus:
- AVAILABLE
- EXPIRED
- UNAVAILABLE
- UNKNOWN
```

## Consequências

- O feed público filtra por `PUBLISHED`.
- A interface exibe badges com base na disponibilidade.
- Promoções publicadas continuam gerando histórico e SEO mesmo após a oferta acabar.