package br.com.descontovivo.moderation.api;

import br.com.descontovivo.moderation.service.PromotionModerationService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.UUID;

@Path("/api/v1/moderation/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
public class ModerationResource {

    private final PromotionModerationService moderationService;

    public ModerationResource(PromotionModerationService moderationService) {
        this.moderationService = moderationService;
    }

    @GET
    public Response list(@QueryParam("status") @DefaultValue("PENDING_REVIEW") String status,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        return Response.ok(moderationService.listByStatus(status, page, size)).build();
    }

    @PATCH
    @Path("/{id}")
    public Response moderate(@PathParam("id") UUID id,
                             @Valid ModerationActionRequest request) {
        return Response.ok(moderationService.moderate(id, request)).build();
    }
}
