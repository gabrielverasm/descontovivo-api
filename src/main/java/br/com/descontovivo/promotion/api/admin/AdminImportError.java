package br.com.descontovivo.promotion.api.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Error detail nested inside {@link AdminImportResponse}.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * as part of a {@code List<AdminImportError>} inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record AdminImportError(
        String sourceId,
        String field,
        String message
) {}
