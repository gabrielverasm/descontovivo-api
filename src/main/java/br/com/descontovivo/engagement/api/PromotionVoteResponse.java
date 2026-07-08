package br.com.descontovivo.engagement.api;

import io.quarkus.runtime.annotations.RegisterForReflection;

/**
 * Response DTO for vote operations.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} body.
 */
@RegisterForReflection
public record PromotionVoteResponse(
        int likesCount,
        int dislikesCount,
        String userVote
) {}
