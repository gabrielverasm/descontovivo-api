package br.com.descontovivo.promotion.api.admin;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.List;

/**
 * Response DTO for the admin import endpoint.
 *
 * <p>{@code @RegisterForReflection} is required because the endpoint returns
 * {@code Response.ok(...)}, which erases generic type information at build time.
 * Without this annotation, GraalVM native image strips the reflection metadata and
 * Jackson serializes the record as {@code {}} (empty object).
 */
@RegisterForReflection
public record AdminImportResponse(
        String batchId,
        boolean dryRun,
        int created,
        int skipped,
        List<AdminImportError> errors
) {}
