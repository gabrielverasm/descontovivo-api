# ADR-0004: Usar Panache Repository

## Status

Aceita

## Contexto

O projeto precisa usar persistência de forma produtiva sem acoplar regra de negócio diretamente à Entity JPA.

Foram consideradas três alternativas:

- Panache Active Record;
- Panache Repository;
- Repository clássico sem Panache.

## Decisão

Usaremos Panache Repository.

## Consequências

- Menos boilerplate que repository clássico.
- Melhor separação que Active Record.
- Entities ficam focadas em persistência.
- Queries ficam concentradas nos repositories.