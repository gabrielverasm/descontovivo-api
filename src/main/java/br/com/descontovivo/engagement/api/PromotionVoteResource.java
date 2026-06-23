package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionVoteService;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/promotions/{slug}/vote")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Authenticated
public class PromotionVoteResource {

    private final PromotionVoteService voteService;

    public PromotionVoteResource(PromotionVoteService voteService) {
        this.voteService = voteService;
    }

    @PUT
    public Response vote(@PathParam("slug") String slug, @Valid PromotionVoteRequest request) {
        return Response.ok(voteService.vote(slug, request.type())).build();
    }

    @DELETE
    public Response removeVote(@PathParam("slug") String slug) {
        return Response.ok(voteService.removeVote(slug)).build();
    }
}
