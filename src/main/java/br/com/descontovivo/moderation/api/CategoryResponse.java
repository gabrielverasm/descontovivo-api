package br.com.descontovivo.moderation.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * DTO returned by the category management endpoints.
 *
 * <p>{@code @RegisterForReflection} is required because the endpoint returns
 * {@code Response.ok(...)}, which erases generic type information at build time.
 * Without this annotation, GraalVM native image strips the reflection metadata and
 * Jackson serializes the record as {@code {}} (empty object).
 */
@RegisterForReflection
public record CategoryResponse(String name, long promotionCount) {}
