package br.com.descontovivo.account.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDataRequestSummary(
        UUID id,
        String type,
        String status,
        OffsetDateTime createdAt,
        OffsetDateTime resolvedAt
) {}
