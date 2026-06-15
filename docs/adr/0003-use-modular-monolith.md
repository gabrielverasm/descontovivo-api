# ADR-0003: Usar monólito modular

## Status

Aceita

## Contexto

O DescontoVivo está em fase de MVP e será mantido inicialmente por uma pessoa/time pequeno.

Microsserviços aumentariam a complexidade operacional antes de existir necessidade real.

## Decisão

Usaremos um monólito modular.

## Consequências

- Deploy simples.
- Menor complexidade de infraestrutura.
- Separação interna por módulos de negócio.
- Possibilidade de extrair serviços no futuro, caso exista necessidade real.