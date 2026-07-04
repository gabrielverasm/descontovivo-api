package br.com.descontovivo.notification.dto;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Lightweight snapshot for admin SSE stream.
 * Contains only the count of open (PENDING + IN_REVIEW) data requests.
 *
 * <p>{@code @RegisterForReflection} is required because this record is never returned
 * directly from a JAX-RS endpoint — it is serialized internally by
 * {@link br.com.descontovivo.notification.api.NotificationPayloadFactory} using ObjectMapper.
 * Without this annotation, GraalVM native image strips the reflection metadata and
 * Jackson serializes the record as {@code {}} (empty object).
 */
@RegisterForReflection
public record AdminDataRequestSnapshot(
        long openCount
) {}
