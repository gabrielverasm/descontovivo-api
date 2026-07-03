package br.com.descontovivo.account.api.admin;

import java.time.OffsetDateTime;
import java.util.UUID;

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
