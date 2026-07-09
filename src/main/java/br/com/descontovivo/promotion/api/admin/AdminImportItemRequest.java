package br.com.descontovivo.promotion.api.admin;

import java.math.BigDecimal;
import java.time.OffsetDateTime;

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
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String coupon,
        String category,
        String availability,
        String priceSignal,
        OffsetDateTime publishAt,
        OffsetDateTime verifiedAt
) {}
