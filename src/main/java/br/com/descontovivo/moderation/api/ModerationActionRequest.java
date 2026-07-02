package br.com.descontovivo.moderation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

public record ModerationActionRequest(
        @NotNull ModerationAction action,
        @NotBlank @Size(max = 500) String reason,
        @Size(max = 180) String title,
        @Size(max = 2048) String url,
        BigDecimal currentPrice,
        BigDecimal originalPrice,
        @Size(max = 80) String couponCode,
        @Size(max = 2048) String imageUrl,
        @Size(max = 200) String imageKey,
        @Size(max = 30) String availability,
        @Size(max = 120) String storeSlug,
        @Size(max = 100) String soldBy,
        @Size(max = 100) String deliveredBy,
        @Size(max = 50) String category
) {
    public enum ModerationAction {
        APPROVE, REJECT, REMOVE, EDIT
    }
}
