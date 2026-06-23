package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.service.PromotionCommentService;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/promotions/{slug}/comments")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionCommentResource {

    private final PromotionCommentService commentService;

    public PromotionCommentResource(PromotionCommentService commentService) {
        this.commentService = commentService;
    }

    @GET
    @PermitAll
    public Response listComments(@PathParam("slug") String slug) {
        return Response.ok(commentService.listByPromotion(slug)).build();
    }

    @POST
    @Authenticated
    public Response createComment(@PathParam("slug") String slug, @Valid PromotionCommentCreateRequest request) {
        return Response.status(Response.Status.CREATED).entity(commentService.createComment(slug, request)).build();
    }
}
