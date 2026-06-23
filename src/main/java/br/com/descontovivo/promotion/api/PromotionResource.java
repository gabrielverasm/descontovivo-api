package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.service.PromotionService;
import br.com.descontovivo.shared.api.PagedResponse;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/v1/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionResource {

    private final PromotionService promotionService;

    public PromotionResource(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GET
    @PermitAll
    public PagedResponse<PromotionSummaryResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("store") String store,
            @QueryParam("availability") String availability,
            @QueryParam("q") String q) {

        size = Math.min(size, 100);
        var items = promotionService.listPublished(page, size, store, availability, q);
        long total = promotionService.countPublished(store, availability, q);
        return PagedResponse.of(items, page, size, total);
    }

    @GET
    @Path("/{slug}")
    @PermitAll
    public PromotionDetailResponse getBySlug(@PathParam("slug") String slug) {
        return promotionService.findPublishedBySlug(slug);
    }

    @POST
    @Authenticated
    public Response create(@Valid PromotionCreateRequest request) {
        return Response.status(201).entity(promotionService.create(request)).build();
    }
}
