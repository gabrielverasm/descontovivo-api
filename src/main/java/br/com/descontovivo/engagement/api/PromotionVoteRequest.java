package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record PromotionVoteRequest(
        @NotNull @Size(max = 20) String type
) {}
