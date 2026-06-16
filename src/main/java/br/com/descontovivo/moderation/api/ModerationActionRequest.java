package br.com.descontovivo.moderation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record ModerationActionRequest(
        @NotNull ModerationAction action,
        @NotBlank String reason,
        String title,
        String url,
        String description,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        String couponCode,
        String imageUrl,
        String availability,
        String storeSlug
) {
    public enum ModerationAction {
        APPROVE, REJECT, REMOVE, EDIT
    }
}
