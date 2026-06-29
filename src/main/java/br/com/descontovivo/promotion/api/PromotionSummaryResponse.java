package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.entity.PromotionEntity;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

public record PromotionSummaryResponse(
        UUID id,
        String slug,
        String title,
        String url,
        String description,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String couponCode,
        String imageUrl,
        String availability,
        StoreRef store,
        String soldBy,
        String deliveredBy,
        String category,
        int likesCount,
        int dislikesCount,
        int commentsCount,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        String authorUsername
) {
    public record StoreRef(String slug, String name) {}

    public static PromotionSummaryResponse from(PromotionEntity e) {
        return new PromotionSummaryResponse(
                e.getId(), e.getSlug(), e.getTitle(), e.getUrl(), e.getDescription(),
                e.getCurrentPrice(), e.getOriginalPrice(), e.getCouponCode(),
                e.getImageUrl(), e.getAvailability().name(),
                new StoreRef(e.getStore().getSlug(), e.getStore().getName()),
                e.getSoldBy(), e.getDeliveredBy(), e.getCategory(),
                e.getLikesCount(), e.getDislikesCount(), e.getCommentsCount(),
                e.getCreatedAt(), e.getPublishedAt(),
                e.getAuthorUsername()
        );
    }
}
