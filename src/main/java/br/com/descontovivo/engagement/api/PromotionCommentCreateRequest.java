package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotBlank;

public record PromotionCommentCreateRequest(
        @NotBlank String authorName,
        @NotBlank String content
) {}
