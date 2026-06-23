package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.service.PromotionService;
import br.com.descontovivo.shared.api.ApiErrorResponse;
import br.com.descontovivo.shared.api.PagedResponse;
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

@Path("/api/v1/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Promotions", description = "Promotion listing, detail and creation")
public class PromotionResource {

    private final PromotionService promotionService;

    public PromotionResource(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GET
    @PermitAll
    @Operation(summary = "List published promotions", description = "Paginated list with optional filters by store, availability and search query")
    @APIResponse(responseCode = "200", description = "Paginated promotions")
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
    @Operation(summary = "Get promotion by slug")
    @APIResponse(responseCode = "200", description = "Promotion detail")
    @APIResponse(responseCode = "404", description = "Promotion not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public PromotionDetailResponse getBySlug(@PathParam("slug") String slug) {
        return promotionService.findPublishedBySlug(slug);
    }

    @POST
    @Authenticated
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Create a new promotion", description = "Requires authenticated user with verified email")
    @APIResponse(responseCode = "201", description = "Promotion created")
    @APIResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Email not verified", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response create(@Valid PromotionCreateRequest request) {
        return Response.status(201).entity(promotionService.create(request)).build();
    }
}
