package br.com.descontovivo.promotion.api.admin;

public record AdminImportError(
        String sourceId,
        String field,
        String message
) {}
