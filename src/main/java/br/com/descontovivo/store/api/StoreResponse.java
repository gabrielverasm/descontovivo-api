package br.com.descontovivo.store.api;

import br.com.descontovivo.store.entity.StoreEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

/**
 * Response DTO for store endpoints.
 *
 * <p>{@code @RegisterForReflection} ensures native image serialization works correctly.
 */
@RegisterForReflection
public record StoreResponse(UUID id, String name, String slug, String url) {

    public static StoreResponse from(StoreEntity entity) {
        return new StoreResponse(entity.getId(), entity.getName(), entity.getSlug(), entity.getUrl());
    }
}
