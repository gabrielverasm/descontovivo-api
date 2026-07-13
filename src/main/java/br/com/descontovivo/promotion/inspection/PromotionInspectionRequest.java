package br.com.descontovivo.promotion.inspection;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromotionInspectionRequest(@NotBlank @Size(max = 2048) String url) {}
