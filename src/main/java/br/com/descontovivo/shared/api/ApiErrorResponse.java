package br.com.descontovivo.shared.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(description = "Standard error response")
public record ApiErrorResponse(
        @Schema(example = "400") int status,
        @Schema(example = "Bad Request") String error,
        @Schema(example = "Validation failed") String message
) {}
