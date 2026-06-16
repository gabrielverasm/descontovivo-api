package br.com.descontovivo.store.api;

import br.com.descontovivo.store.entity.StoreEntity;

import java.util.UUID;

public record StoreResponse(UUID id, String name, String slug, String url) {

    public static StoreResponse from(StoreEntity entity) {
        return new StoreResponse(entity.getId(), entity.getName(), entity.getSlug(), entity.getUrl());
    }
}
