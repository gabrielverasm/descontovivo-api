package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.entity.PromotionEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary of a promotion for listing pages.
 *
 * <p>{@code @RegisterForReflection} is required because this record is nested inside
 * {@link PagedResponse} (a generic type) and returned via endpoints.
 */
@RegisterForReflection
public record PromotionSummaryResponse(
        UUID id,
        String slug,
        String title,
        String url,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String couponCode,
        String imageUrl,
        String availability,
        StoreRef store,
        String soldBy,
        String deliveredBy,
        String category,
        String priceSignal,
        int likesCount,
        int dislikesCount,
        int commentsCount,
        OffsetDateTime createdAt,
        OffsetDateTime publishedAt,
        String authorUsername
) {
    @RegisterForReflection
    public record StoreRef(String slug, String name) {}

    public static PromotionSummaryResponse from(PromotionEntity e) {
        return new PromotionSummaryResponse(
                e.getId(), e.getSlug(), e.getTitle(), e.getUrl(),
                e.getCurrentPrice(), e.getOriginalPrice(), e.getCouponCode(),
                e.getImageUrl(), e.getAvailability().name(),
                new StoreRef(e.getStore().getSlug(), e.getStore().getName()),
                e.getSoldBy(), e.getDeliveredBy(), e.getCategory(),
                e.getPriceSignal() != null ? e.getPriceSignal().name() : "NONE",
                e.getLikesCount(), e.getDislikesCount(), e.getCommentsCount(),
                e.getCreatedAt(), e.getPublishedAt(),
                e.getAuthorUsername()
        );
    }
}
