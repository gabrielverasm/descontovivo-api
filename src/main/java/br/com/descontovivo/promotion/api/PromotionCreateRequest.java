package br.com.descontovivo.promotion.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PromotionCreateRequest(
        @NotBlank String title,
        @NotBlank String url,
        @NotBlank String description,
        @NotNull @DecimalMin("0.01") BigDecimal currentPrice,
        BigDecimal originalPrice,
        String couponCode,
        @NotBlank String imageUrl,
        @NotBlank String storeSlug
) {}
