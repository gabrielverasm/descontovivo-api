package br.com.descontovivo.promotion.inspection;

import java.math.BigDecimal;
import java.util.List;

public record InternalShopeeInspectionResponse(
        String marketplace, String inputUrl, String productUrl, String affiliateUrl,
        Long shopId, Long itemId, String title, BigDecimal currentPrice,
        BigDecimal originalPrice, String remoteImageUrl, String storeName,
        String sellerName, String soldBy, String deliveredBy, Long salesCount,
        Double productRating, Double sellerRating, boolean officialStore,
        boolean shopeeGuarantee, String category, List<String> missingFields,
        List<String> warnings) {}
