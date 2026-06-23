package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import br.com.descontovivo.shared.api.ApiErrorResponse;
import jakarta.annotation.security.RolesAllowed;
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

@Path("/api/v1/moderation/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Moderation", description = "Content moderation by moderators/admins")
public class CommentModerationResource {

    private final PromotionCommentService commentService;

    public CommentModerationResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Moderate a comment", description = "Hide or remove a comment. Requires moderator or admin role.")
    @APIResponse(responseCode = "200", description = "Comment moderated")
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Comment not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response moderate(@PathParam("id") UUID id,
                             @Valid CommentModerationRequest request) {
        return Response.ok(commentService.moderateComment(id, request.reason())).build();
    }
}
