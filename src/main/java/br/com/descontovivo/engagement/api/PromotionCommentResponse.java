package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.entity.PromotionCommentEntity;
import io.quarkus.runtime.annotations.RegisterForReflection;

import java.time.OffsetDateTime;
import java.util.UUID;

/**
 * Response DTO for comments on promotions.
 *
 * <p>{@code @RegisterForReflection} is required because this record is serialized
 * inside a {@code Response.ok()} / {@code Response.status().entity()} body.
 */
@RegisterForReflection
public record PromotionCommentResponse(
        UUID id,
        UUID parentId,
        String authorName,
        String content,
        boolean removed,
        OffsetDateTime createdAt
) {
    public static PromotionCommentResponse from(PromotionCommentEntity e) {
        return new PromotionCommentResponse(
                e.getId(),
                e.getParent() != null ? e.getParent().getId() : null,
                e.getAuthorName(),
                e.isRemoved() ? "Comentário removido" : e.getContent(),
                e.isRemoved(),
                e.getCreatedAt()
        );
    }
}
