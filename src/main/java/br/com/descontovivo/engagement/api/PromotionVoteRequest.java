package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromotionVoteRequest(
        @NotBlank String clientId,
        @NotNull String type
) {}
