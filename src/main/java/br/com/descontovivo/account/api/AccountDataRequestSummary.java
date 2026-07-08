package br.com.descontovivo.account.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Summary DTO for data request listings.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record AccountDataRequestSummary(
        UUID id,
        String type,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {}
