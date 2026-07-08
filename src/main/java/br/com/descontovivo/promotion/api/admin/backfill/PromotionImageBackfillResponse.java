package br.com.descontovivo.promotion.api.admin.backfill;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Response DTO for the image backfill admin endpoint.
 *
 * <p>{@code @RegisterForReflection} is required because the endpoint returns
 * {@code Response.ok(...)}, which erases generic type information at build time.
 */
@RegisterForReflection
public record PromotionImageBackfillResponse(
        boolean dryRun,
        int scanned,
        int eligible,
        int updated,
        int failed,
        List<PromotionImageBackfillItem> items
) {}
