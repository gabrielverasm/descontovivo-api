package br.com.descontovivo.moderation.api;

import br.com.descontovivo.moderation.service.PromotionModerationService;
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

@Path("/api/v1/moderation/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Moderation", description = "Content moderation by moderators/admins")
public class ModerationResource {

    private final PromotionModerationService moderationService;

    public ModerationResource(PromotionModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GET
    @Operation(summary = "List promotions pending moderation")
    @APIResponse(responseCode = "200", description = "Paginated list of promotions for moderation")
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response list(@QueryParam("status") @DefaultValue("PENDING_REVIEW") String status,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        return Response.ok(moderationService.listByStatus(status, page, size)).build();
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Moderate a promotion", description = "Approve, reject, remove or edit a promotion")
    @APIResponse(responseCode = "200", description = "Moderation action applied")
    @APIResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "409", description = "Conflict with current state", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response moderate(@PathParam("id") UUID id,
                             @Valid ModerationActionRequest request) {
        return Response.ok(moderationService.moderate(id, request)).build();
    }
}
