package br.com.descontovivo.engagement.repository;

import br.com.descontovivo.engagement.entity.PromotionVoteEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PromotionVoteRepository implements PanacheRepositoryBase<PromotionVoteEntity, UUID> {

    public Optional<PromotionVoteEntity> findByPromotionAndClient(UUID promotionId, String clientId) {
        return find("promotion.id = ?1 and clientId = ?2", promotionId, clientId).firstResultOptional();
    }
}
