package br.com.descontovivo.promotion.inspection;

import java.math.BigDecimal;
import java.util.List;

public record PromotionInspectionResponse(
        MarketplaceCode marketplace, boolean supported, String inputUrl, String productUrl,
        String affiliateUrl, String title, BigDecimal currentPrice, BigDecimal originalPrice,
        String imageKey, String imageUrl, String storeName, String sellerName, String soldBy,
        String deliveredBy, Long salesCount, Double productRating, Double sellerRating,
        boolean officialStore, boolean shopeeGuarantee, String category,
        List<String> trustSignals, List<String> missingFields, List<String> warnings) {}
