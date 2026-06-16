package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.repository.PromotionCommentRepository;
import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import br.com.descontovivo.moderation.repository.ModerationLogRepository;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/api/v1/moderation/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class CommentModerationResource {

    @ConfigProperty(name = "app.admin-token", defaultValue = "dev-admin-token")
    String adminToken;

    private final PromotionCommentRepository commentRepository;
    private final ModerationLogRepository moderationLogRepository;

    public CommentModerationResource(PromotionCommentRepository commentRepository,
                                     ModerationLogRepository moderationLogRepository) {
        this.commentRepository = commentRepository;
        this.moderationLogRepository = moderationLogRepository;
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    public Response moderate(@HeaderParam("X-Admin-Token") String token,
                             @PathParam("id") UUID id,
                             CommentModerationRequest request) {
        if (token == null || !token.trim().equals(adminToken.trim())) {
            throw new ForbiddenException("Invalid or missing admin token");
        }

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
        log.setActor("admin");
        log.setCreatedAt(now);
        moderationLogRepository.persist(log);

        return Response.ok(PromotionCommentResponse.from(comment)).build();
    }
}
