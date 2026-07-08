package br.com.descontovivo.account.api.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Admin view of a data request.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record AccountDataRequestAdminSummary(
        UUID id,
        String userSubject,
        String username,
        String email,
        String displayName,
        String type,
        String status,
        String details,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt,
        OffsetDateTime resolvedAt,
        String resolutionNote
) {}
