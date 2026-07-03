package br.com.descontovivo.notification.dto;

/**
 * Lightweight snapshot for admin/moderator SSE stream.
 * Contains only the count of promotions pending moderation.
 */
public record ModerationPromotionSnapshot(
        long pendingCount
) {}
