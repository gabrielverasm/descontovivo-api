package br.com.descontovivo.notification.dto;

/**
 * Lightweight snapshot for admin SSE stream.
 * Contains only the count of open (PENDING + IN_REVIEW) data requests.
 */
public record AdminDataRequestSnapshot(
        long openCount
) {}
