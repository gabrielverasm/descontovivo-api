package br.com.descontovivo.upload.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record PresignUploadRequest(
        @NotBlank String contentType,
        @NotNull @Positive Long fileSize
) {}
