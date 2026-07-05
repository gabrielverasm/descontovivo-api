package br.com.descontovivo.moderation.api;

import io.quarkus.runtime.annotations.RegisterForReflection;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request body for renaming a category.
 *
 * <p>{@code @RegisterForReflection} is required because this record is deserialized
 * from request bodies in endpoints that use raw {@code Response} return types.
 * Without this annotation, GraalVM native image may fail to deserialize the record.
 */
@RegisterForReflection
public record RenameCategoryRequest(
        @NotBlank @Size(max = 50) String name
) {}
