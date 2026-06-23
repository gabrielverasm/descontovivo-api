package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotNull;

public record PromotionVoteRequest(
        @NotNull String type
) {}
