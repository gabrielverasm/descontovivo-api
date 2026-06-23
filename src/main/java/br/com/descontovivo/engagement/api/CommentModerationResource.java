package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/moderation/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
public class CommentModerationResource {

    private final PromotionCommentService commentService;

    public CommentModerationResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @PATCH
    @Path("/{id}")
    public Response moderate(@PathParam("id") UUID id,
                             CommentModerationRequest request) {
        return Response.ok(commentService.moderateComment(id, request.reason())).build();
    }
}
