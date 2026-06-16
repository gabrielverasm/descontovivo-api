package br.com.descontovivo.engagement.repository;

import br.com.descontovivo.engagement.entity.PromotionCommentEntity;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PromotionCommentRepository implements PanacheRepositoryBase<PromotionCommentEntity, UUID> {

    public List<PromotionCommentEntity> listByPromotion(UUID promotionId) {
        return find("promotion.id = ?1", Sort.by("createdAt").ascending(), promotionId).list();
    }
}
