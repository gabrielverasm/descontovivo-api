package br.com.descontovivo.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

/**
 * Standard error response returned by exception mappers.
 *
 * <p>{@code @RegisterForReflection} is required because exception mappers return
 * {@code Response.status().entity(new ApiErrorResponse(...))}, which erases type info.
 */
@RegisterForReflection
@Schema(description = "Standard error response")
public record ApiErrorResponse(
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed") String message
) {}
