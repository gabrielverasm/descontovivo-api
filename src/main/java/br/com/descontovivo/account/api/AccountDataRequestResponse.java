package br.com.descontovivo.account.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for data request creation.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.status().entity()} body.
 */
@RegisterForReflection
public record AccountDataRequestResponse(
        UUID id,
        String type,
        String status,
        OffsetDateTime createdAt,
        String message
) {}
