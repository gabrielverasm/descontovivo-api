package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import br.com.descontovivo.shared.api.ApiErrorResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
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

@Path("/api/v1/promotions/{slug}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Comments", description = "Comments on promotions")
public class PromotionCommentResource {

    private final PromotionCommentService commentService;

    public PromotionCommentResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @GET
    @PermitAll
    @Operation(summary = "List comments for a promotion")
    @APIResponse(responseCode = "200", description = "List of comments")
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response listComments(@PathParam("slug") String slug) {
        return Response.ok(commentService.listByPromotion(slug)).build();
    }

    @POST
    @Authenticated
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Add a comment to a promotion", description = "Requires authenticated user with verified email")
    @APIResponse(responseCode = "201", description = "Comment created")
    @APIResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Email not verified", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response createComment(@PathParam("slug") String slug, @Valid PromotionCommentCreateRequest request) {
        return Response.status(Response.Status.CREATED).entity(commentService.createComment(slug, request)).build();
    }
}
