package br.com.descontovivo.upload.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for presigned upload URL generation.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record PresignUploadResponse(
        String uploadUrl,
        String publicUrl,
        String objectKey,
        int expiresInSeconds
) {}
