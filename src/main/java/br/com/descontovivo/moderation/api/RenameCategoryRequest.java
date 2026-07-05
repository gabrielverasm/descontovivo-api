package br.com.descontovivo.moderation.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RenameCategoryRequest(
        @NotBlank @Size(max = 50) String name
) {}
