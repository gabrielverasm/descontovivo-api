package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionVoteService;
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

@Path("/api/v1/promotions/{slug}/vote")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Engagement", description = "Votes and interactions on promotions")
public class PromotionVoteResource {

    private final PromotionVoteService voteService;

    public PromotionVoteResource(PromotionVoteService voteService) {
        this.voteService = voteService;
    }

    @PUT
    @Operation(summary = "Vote on a promotion")
    @APIResponse(responseCode = "200", description = "Vote registered")
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Email not verified", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "422", description = "Invalid vote type", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response vote(@PathParam("slug") String slug, @Valid PromotionVoteRequest request) {
        return Response.ok(voteService.vote(slug, request.type())).build();
    }

    @DELETE
    @Operation(summary = "Remove vote from a promotion")
    @APIResponse(responseCode = "200", description = "Vote removed")
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response removeVote(@PathParam("slug") String slug) {
        return Response.ok(voteService.removeVote(slug)).build();
    }
}
