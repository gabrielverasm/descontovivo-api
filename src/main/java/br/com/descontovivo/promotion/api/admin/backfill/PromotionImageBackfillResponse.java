package br.com.descontovivo.promotion.api.admin.backfill;

import java.util.List;

public record PromotionImageBackfillResponse(
        boolean dryRun,
        int scanned,
        int eligible,
        int updated,
        int failed,
        List<PromotionImageBackfillItem> items
) {}
