package br.com.descontovivo.account.api;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AccountDataRequestResponse(
        UUID id,
        String type,
        String status,
        OffsetDateTime createdAt,
        String message
) {}
