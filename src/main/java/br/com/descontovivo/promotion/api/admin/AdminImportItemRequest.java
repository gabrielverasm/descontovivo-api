package br.com.descontovivo.promotion.api.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.List;

public record AdminImportItemRequest(
        String sourceId,
        String title,
        String marketplace,
        String storeName,
        String sellerName,
        String soldBy,
        String deliveredBy,
        String productUrl,
        String imageUrl,
        String imageKey,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String coupon,
        String category,
        String availability,
        String priceSignal,
        OffsetDateTime publishAt,
        OffsetDateTime verifiedAt,
        // New trust signals fields
        Integer salesCount,
        BigDecimal productRating,
        BigDecimal sellerRating,
        Boolean officialStore,
        List<String> trustSignals
) {}
