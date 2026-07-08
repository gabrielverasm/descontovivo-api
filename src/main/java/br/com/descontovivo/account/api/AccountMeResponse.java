package br.com.descontovivo.account.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.util.Set;

/**
 * Response DTO for the /me endpoint.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record AccountMeResponse(
        String subject,
        String username,
        String email,
        boolean emailVerified,
        Set<String> roles
) {}
