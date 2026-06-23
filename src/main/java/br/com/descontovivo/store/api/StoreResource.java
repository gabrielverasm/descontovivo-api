package br.com.descontovivo.store.api;

import br.com.descontovivo.shared.api.ApiErrorResponse;
import br.com.descontovivo.store.repository.StoreRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/stores")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
@Tag(name = "Stores", description = "Store catalog")
public class StoreResource {

    private final StoreRepository repository;

    public StoreResource(StoreRepository repository) {
        this.repository = repository;
    }

    @GET
    @Operation(summary = "List all stores")
    @APIResponse(responseCode = "200", description = "List of stores")
    public List<StoreResponse> list() {
        return repository.listAll().stream().map(StoreResponse::from).toList();
    }

    @GET
    @Path("/{slug}")
    @Operation(summary = "Get store by slug")
    @APIResponse(responseCode = "200", description = "Store detail")
    @APIResponse(responseCode = "404", description = "Store not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public StoreResponse getBySlug(@PathParam("slug") String slug) {
        return repository.findBySlug(slug)
                .map(StoreResponse::from)
                .orElseThrow(() -> new NotFoundException("Store not found: " + slug));
    }
}
