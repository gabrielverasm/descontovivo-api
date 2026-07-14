package br.com.descontovivo.promotion.inspection;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@RegisterForReflection
public record PromotionInspectionRequest(@NotBlank @Size(max = 2048) String url) {}
