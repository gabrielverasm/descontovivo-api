package br.com.descontovivo.upload.api;

public record PresignUploadResponse(
        String uploadUrl,
        String publicUrl,
        String objectKey,
        int expiresInSeconds
) {}
