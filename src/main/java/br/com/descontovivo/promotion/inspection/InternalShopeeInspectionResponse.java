package br.com.descontovivo.promotion.inspection;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.util.List;

@RegisterForReflection
public record InternalShopeeInspectionResponse(
        String marketplace, String inputUrl, String productUrl, String affiliateUrl,
        Long shopId, Long itemId, String title, BigDecimal currentPrice,
        BigDecimal originalPrice, String remoteImageUrl, String storeName,
        String sellerName, String soldBy, String deliveredBy, Long salesCount,
        Double productRating, Double sellerRating, boolean officialStore,
        boolean shopeeGuarantee, String category, List<String> missingFields,
        List<String> warnings) {}
