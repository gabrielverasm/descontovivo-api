package br.com.descontovivo.notification.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;

/**
 * Lightweight snapshot for public SSE stream.
 * Contains only aggregate/public data — no sensitive information.
 *
 * <p>{@code @RegisterForReflection} is required because this record is never returned
 * directly from a JAX-RS endpoint — it is serialized internally by
 * {@link br.com.descontovivo.notification.api.NotificationPayloadFactory} using ObjectMapper.
 * Without this annotation, GraalVM native image strips the reflection metadata and
 * Jackson serializes the record as {@code {}} (empty object).
 */
@RegisterForReflection
public record PublicPromotionSnapshot(
        long publishedCount,
        OffsetDateTime latestPublishedAt
) {}
