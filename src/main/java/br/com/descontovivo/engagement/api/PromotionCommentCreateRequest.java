package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PromotionCommentCreateRequest(
        @NotBlank @Size(max = 120) String authorName,
        @NotBlank @Size(max = 2000) String content
) {}
