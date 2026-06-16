package br.com.descontovivo.store.repository;

import br.com.descontovivo.store.entity.StoreEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class StoreRepository implements PanacheRepositoryBase<StoreEntity, UUID> {

    public Optional<StoreEntity> findBySlug(String slug) {
        return find("slug", slug).firstResultOptional();
    }
}
