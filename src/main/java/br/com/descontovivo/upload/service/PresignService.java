package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.api.PresignUploadRequest;
import br.com.descontovivo.upload.api.PresignUploadResponse;
import br.com.descontovivo.upload.config.R2Config;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PresignService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final S3Presigner presigner;
    private final R2Config r2Config;
    private final int expirationSeconds;
    private final long maxFileSize;
    private final List<String> allowedContentTypes;

    public PresignService(
            S3Presigner presigner,
            R2Config r2Config,
            @ConfigProperty(name = "upload.presign-expiration-seconds") int expirationSeconds,
            @ConfigProperty(name = "upload.max-file-size") long maxFileSize,
            @ConfigProperty(name = "upload.allowed-content-types") List<String> allowedContentTypes
    ) {
        this.presigner = presigner;
        this.r2Config = r2Config;
        this.expirationSeconds = expirationSeconds;
        this.maxFileSize = maxFileSize;
        this.allowedContentTypes = allowedContentTypes;
    }

    public PresignUploadResponse generatePresignedUrl(PresignUploadRequest request) {
        validateContentType(request.contentType());
        validateFileSize(request.fileSize());

        String objectKey = buildObjectKey();

        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(r2Config.bucket())
                .key(objectKey)
                .contentType("image/webp")
                .contentLength(request.fileSize())
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .putObjectRequest(putRequest)
                .build();

        String uploadUrl = presigner.presignPutObject(presignRequest).url().toString();
        String publicUrl = r2Config.publicBaseUrl() + "/" + objectKey;

        return new PresignUploadResponse(uploadUrl, publicUrl, objectKey, expirationSeconds);
    }

    private String buildObjectKey() {
        String prefix = LocalDate.now().format(YEAR_MONTH);
        return "temp/promotions/" + prefix + "/" + UUID.randomUUID() + ".webp";
    }

    private void validateContentType(String contentType) {
        if (!allowedContentTypes.contains(contentType)) {
            throw new IllegalArgumentException(
                    "Only image/webp is accepted. Convert the image before uploading.");
        }
    }

    private void validateFileSize(long fileSize) {
        if (fileSize > maxFileSize) {
            throw new IllegalArgumentException(
                    "File size exceeds maximum allowed (" + maxFileSize + " bytes)");
        }
    }
}
