package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.entity.PromotionEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromotionDetailResponse(
        UUID id,
        String slug,
        String title,
        String url,
        String description,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String couponCode,
        String imageUrl,
        String status,
        String availability,
        PromotionSummaryResponse.StoreRef store,
        String soldBy,
        String deliveredBy,
        String category,
        int likesCount,
        int dislikesCount,
        int commentsCount,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt
) {
    public static PromotionDetailResponse from(PromotionEntity e) {
        return new PromotionDetailResponse(
                e.getId(), e.getSlug(), e.getTitle(), e.getUrl(), e.getDescription(),
                e.getCurrentPrice(), e.getOriginalPrice(), e.getCouponCode(),
                e.getImageUrl(), e.getStatus().name(), e.getAvailability().name(),
                new PromotionSummaryResponse.StoreRef(e.getStore().getSlug(), e.getStore().getName()),
                e.getSoldBy(), e.getDeliveredBy(), e.getCategory(),
                e.getLikesCount(), e.getDislikesCount(), e.getCommentsCount(),
                e.getCreatedAt(), e.getPublishedAt()
        );
    }
}
