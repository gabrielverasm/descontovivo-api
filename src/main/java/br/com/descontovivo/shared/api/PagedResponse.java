package br.com.descontovivo.shared.api;

import io.quarkus.runtime.annotations.RegisterForReflection;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

/**
 * Paginated response wrapper.
 *
 * <p>{@code @RegisterForReflection} is required because this generic record may not
 * have its type parameters resolved at build time in native image.
 */
@RegisterForReflection
@Schema(description = "Paginated response wrapper")
public record PagedResponse<T>(
        List<T> content,
        @Schema(example = "0") int page,
        @Schema(example = "20") int size,
        @Schema(example = "42") long totalElements,
        @Schema(example = "3") int totalPages
) {
    public static <T> PagedResponse<T> of(List<T> content, int page, int size, long totalElements) {
        int totalPages = size > 0 ? (int) Math.ceil((double) totalElements / size) : 0;
        return new PagedResponse<>(content, page, size, totalElements, totalPages);
    }
}
