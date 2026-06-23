package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.entity.PromotionCommentEntity;
import br.com.descontovivo.engagement.repository.PromotionCommentRepository;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;

@Path("/api/v1/promotions/{slug}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionCommentResource {

    private final PromotionRepository promotionRepository;
    private final PromotionCommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;

    public PromotionCommentResource(PromotionRepository promotionRepository,
                                    PromotionCommentRepository commentRepository,
                                    CurrentUserProvider currentUserProvider) {
        this.promotionRepository = promotionRepository;
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GET
    @PermitAll
    public Response listComments(@PathParam("slug") String slug) {
        var promotion = findPublished(slug);
        var comments = commentRepository.listByPromotion(promotion.getId());
        return Response.ok(comments.stream().map(PromotionCommentResponse::from).toList()).build();
    }

    @POST
    @Transactional
    @Authenticated
    public Response createComment(@PathParam("slug") String slug, @Valid PromotionCommentCreateRequest request) {
        var user = currentUserProvider.requireVerifiedUser();
        var promotion = findPublished(slug);
        var now = OffsetDateTime.now();

        var comment = new PromotionCommentEntity();
        comment.setPromotion(promotion);
        comment.setClientId(user.subject());
        comment.setAuthorName(request.authorName());
        comment.setContent(request.content());
        comment.setRemoved(false);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentRepository.persist(comment);

        promotion.setCommentsCount(promotion.getCommentsCount() + 1);

        return Response.status(Response.Status.CREATED).entity(PromotionCommentResponse.from(comment)).build();
    }

    private PromotionEntity findPublished(String slug) {
        return promotionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Published promotion not found: " + slug));
    }
}
