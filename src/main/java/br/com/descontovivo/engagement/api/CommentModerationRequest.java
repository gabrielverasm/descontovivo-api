package br.com.descontovivo.engagement.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CommentModerationRequest(
        @Size(max = 30) String action,
        @NotBlank @Size(max = 500) String reason
) {}
