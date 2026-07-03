package br.com.descontovivo.notification.dto;

import java.time.OffsetDateTime;

/**
 * Lightweight snapshot for public SSE stream.
 * Contains only aggregate/public data — no sensitive information.
 */
public record PublicPromotionSnapshot(
        long publishedCount,
        OffsetDateTime latestPublishedAt
) {}
