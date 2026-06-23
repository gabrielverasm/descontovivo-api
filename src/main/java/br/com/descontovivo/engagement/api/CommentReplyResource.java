package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/comments/{id}/replies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class CommentReplyResource {

    private final PromotionCommentService commentService;

    public CommentReplyResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @POST
    public Response reply(@PathParam("id") UUID id, @Valid PromotionCommentCreateRequest request) {
        return Response.status(Response.Status.CREATED).entity(commentService.createReply(id, request)).build();
    }
}
