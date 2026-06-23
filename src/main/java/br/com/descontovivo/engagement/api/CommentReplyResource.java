package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.entity.PromotionCommentEntity;
import br.com.descontovivo.engagement.repository.PromotionCommentRepository;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import io.quarkus.security.Authenticated;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/api/v1/comments/{id}/replies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CommentReplyResource {

    private final PromotionCommentRepository commentRepository;
    private final CurrentUserProvider currentUserProvider;

    public CommentReplyResource(PromotionCommentRepository commentRepository,
                                CurrentUserProvider currentUserProvider) {
        this.commentRepository = commentRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @POST
    @Transactional
    public Response reply(@PathParam("id") UUID id, @Valid PromotionCommentCreateRequest request) {
        var user = currentUserProvider.requireVerifiedUser();

        var parent = commentRepository.findById(id);
        if (parent == null) throw new NotFoundException("Comment not found: " + id);

        var promotion = parent.getPromotion();
        if (promotion.getStatus() != PromotionStatus.PUBLISHED) {
            throw new NotFoundException("Promotion not published");
        }

        var now = OffsetDateTime.now();
        var reply = new PromotionCommentEntity();
        reply.setPromotion(promotion);
        reply.setParent(parent);
        reply.setClientId(user.subject());
        reply.setAuthorName(request.authorName());
        reply.setContent(request.content());
        reply.setRemoved(false);
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        commentRepository.persist(reply);

        promotion.setCommentsCount(promotion.getCommentsCount() + 1);

        return Response.status(Response.Status.CREATED).entity(PromotionCommentResponse.from(reply)).build();
    }
}
