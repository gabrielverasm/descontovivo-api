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

## Campos da promoção

- Título: obrigatório.
- Link (URL): obrigatório.
- Descrição: obrigatória.
- Loja: obrigatória.
- Categoria: opcional no MVP.
- Preço atual: obrigatório.
- Preço original: opcional.
- Cupom: opcional.
- Imagem: obrigatória no MVP.
- Busca automática de imagem por crawler/robô está fora do MVP.

## Duplicidade de promoção

- É permitido republicar uma promoção com o mesmo link em outro dia.
- Não é permitido publicar a mesma promoção, com mesmo link normalizado e descrição normalizada, no mesmo dia.
- A comparação de "mesmo dia" deve considerar o fuso horário oficial da aplicação (America/Sao_Paulo).

### Normalização de URL

- Remove parâmetros de tracking (utm_*, fbclid, gclid, etc.).
- Remove barra final.
- Normaliza domínio para minúsculo.

### Normalização de descrição

- Ignora caixa (case-insensitive).
- Remove acentos.
- Colapsa espaços duplicados em espaço único.

## Slug

- Slug será gerado automaticamente a partir do título.
- Se já existir slug idêntico, adicionar sufixo curto derivado do UUID.
- Slug é identificador público/SEO, não regra de duplicidade.

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
- Comentário removido não deve ser apagado fisicamente (soft delete).
- A API deve retornar o estado removido para que a interface exiba "comentário removido".

## Moderação

- Moderador pode aprovar, rejeitar, remover e editar promoção.
- Toda ação de moderação deve registrar:
    - promoção;
    - moderador;
    - decisão;
    - motivo;
    - data/hora.
- Edição feita por moderador também deve ser registrada como ação de moderação.

## Edição pelo usuário

- Usuário comum não pode editar promoção pendente.
- Usuário comum não pode editar promoção publicada.
- Correções devem ser solicitadas e passam pela moderação.

## Autenticação

- A autenticação será feita com Keycloak/OIDC.
- O frontend usará Authorization Code Flow com PKCE.
- A API validará JWT Bearer Token.
- A API não armazenará senha de usuário.
