# Regras de Negócio do MVP

## Promoções

- Toda promoção criada por usuário entra como `PENDING_REVIEW`.
- Promoções pendentes não aparecem no feed público.
- Apenas moderador ou administrador pode publicar, rejeitar ou remover promoções.
- Quando uma promoção é aprovada, seu status passa para `PUBLISHED`.
- Promoções `PUBLISHED` aparecem no feed público.
- Promoções `REJECTED` não aparecem publicamente.
- Promoções `REMOVED` não aparecem publicamente.
- Uma promoção só deve ser publicada se, no momento da aprovação, parecer válida.
- Depois de publicada, mesmo que a oferta acabe, a promoção continua visível no feed/histórico.
- Quando a oferta acabar, a disponibilidade muda para `EXPIRED`, `UNAVAILABLE` ou `UNKNOWN`.

## Disponibilidade

- `AVAILABLE`: oferta aparentemente ativa.
- `EXPIRED`: oferta encerrada.
- `UNAVAILABLE`: produto indisponível ou link sem oferta válida.
- `UNKNOWN`: sistema não conseguiu confirmar disponibilidade.

## Votos

- Um usuário pode votar uma única vez por promoção.
- O usuário pode alternar entre like e dislike.
- O usuário pode remover o próprio voto.

## Comentários

- Comentários pertencem a uma promoção publicada.
- Respostas pertencem a um comentário pai.
- Comentários podem ser removidos por moderação.

## Moderação

- Toda ação de moderação deve registrar:
    - promoção;
    - moderador;
    - decisão;
    - motivo;
    - data/hora.

## Autenticação

- A autenticação será feita com Keycloak/OIDC.
- O frontend usará Authorization Code Flow com PKCE.
- A API validará JWT Bearer Token.
- A API não armazenará senha de usuário.