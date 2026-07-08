package br.com.descontovivo.promotion.api.admin.backfill;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.UUID;

/**
 * Individual item detail in {@link PromotionImageBackfillResponse}.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * as part of a {@code List} inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record PromotionImageBackfillItem(
        UUID promotionId,
        String slug,
        String title,
        String oldImageUrl,
        String newImageUrl,
        String imageKey,
        String status,
        String error
) {

    public static PromotionImageBackfillItem success(UUID id, String slug, String title,
                                                     String oldImageUrl, String newImageUrl, String imageKey) {
        return new PromotionImageBackfillItem(id, slug, title, oldImageUrl, newImageUrl, imageKey, "UPDATED", null);
    }

    public static PromotionImageBackfillItem failed(UUID id, String slug, String title,
                                                    String oldImageUrl, String error) {
        return new PromotionImageBackfillItem(id, slug, title, oldImageUrl, null, null, "FAILED", error);
    }

    public static PromotionImageBackfillItem eligible(UUID id, String slug, String title, String oldImageUrl) {
        return new PromotionImageBackfillItem(id, slug, title, oldImageUrl, null, null, "ELIGIBLE", null);
    }
}
