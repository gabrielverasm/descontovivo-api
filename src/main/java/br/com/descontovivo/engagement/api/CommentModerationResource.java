package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.repository.PromotionCommentRepository;
import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import br.com.descontovivo.moderation.repository.ModerationLogRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import jakarta.annotation.security.RolesAllowed;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/api/v1/moderation/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
public class CommentModerationResource {

    private final PromotionCommentRepository commentRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final CurrentUserProvider currentUserProvider;

    public CommentModerationResource(PromotionCommentRepository commentRepository,
                                     ModerationLogRepository moderationLogRepository,
                                     CurrentUserProvider currentUserProvider) {
        this.commentRepository = commentRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    public Response moderate(@PathParam("id") UUID id,
                             CommentModerationRequest request) {
        var user = currentUserProvider.currentUser();

        var comment = commentRepository.findById(id);
        if (comment == null) throw new NotFoundException("Comment not found: " + id);

        var now = OffsetDateTime.now();
        comment.setRemoved(true);
        comment.setRemovedAt(now);
        comment.setUpdatedAt(now);

        var log = new ModerationLogEntity();
        log.setTargetType("COMMENT");
        log.setTargetId(id);
        log.setAction("REMOVE");
        log.setReason(request.reason());
        log.setActor(user.username() != null ? user.username() : user.subject());
        log.setCreatedAt(now);
        moderationLogRepository.persist(log);

        return Response.ok(PromotionCommentResponse.from(comment)).build();
    }
}
