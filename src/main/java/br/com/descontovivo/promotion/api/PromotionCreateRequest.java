package br.com.descontovivo.promotion.api;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record PromotionCreateRequest(
        @NotBlank @Size(max = 180) String title,
        @NotBlank @Size(max = 2048) String url,
        @Size(max = 2000) String description,
        @NotNull @DecimalMin("0.01") BigDecimal currentPrice,
        BigDecimal originalPrice,
        @Size(max = 80) String couponCode,
        @NotBlank @Size(max = 2048) String imageUrl,
        @NotBlank @Size(max = 200) String imageKey,
        @Size(max = 120) String storeSlug
) {}
