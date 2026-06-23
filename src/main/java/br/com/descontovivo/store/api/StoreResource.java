package br.com.descontovivo.store.api;

import br.com.descontovivo.store.repository.StoreRepository;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/v1/stores")
@Produces(MediaType.APPLICATION_JSON)
@PermitAll
public class StoreResource {

    private final StoreRepository repository;

    public StoreResource(StoreRepository repository) {
        this.repository = repository;
    }

    @GET
    public List<StoreResponse> list() {
        return repository.listAll().stream().map(StoreResponse::from).toList();
    }

    @GET
    @Path("/{slug}")
    public StoreResponse getBySlug(@PathParam("slug") String slug) {
        return repository.findBySlug(slug)
                .map(StoreResponse::from)
                .orElseThrow(() -> new NotFoundException("Store not found: " + slug));
    }
}
