package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import br.com.descontovivo.shared.api.ApiErrorResponse;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.UUID;

@Path("/api/v1/comments/{id}/replies")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Comments", description = "Comments on promotions")
public class CommentReplyResource {

    private final PromotionCommentService commentService;

    public CommentReplyResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @POST
    @Operation(summary = "Reply to a comment")
    @APIResponse(responseCode = "201", description = "Reply created")
    @APIResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Email not verified", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Parent comment not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response reply(@PathParam("id") UUID id, @Valid PromotionCommentCreateRequest request) {
        return Response.status(Response.Status.CREATED).entity(commentService.createReply(id, request)).build();
    }
}
