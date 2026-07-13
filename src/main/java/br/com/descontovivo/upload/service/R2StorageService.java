package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.config.R2Config;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.CopyObjectRequest;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.regex.Pattern;

@ApplicationScoped
public class R2StorageService {

    private static final Logger LOG = Logger.getLogger(R2StorageService.class);
    private static final String TEMP_PREFIX = "temp/promotions/";
    private static final String FINAL_PREFIX = "promotions/";
    private static final Pattern TEMP_KEY = Pattern.compile(
            "^temp/promotions/\\d{4}/\\d{2}/[a-f0-9-]+\\.webp$");

    private final S3Client s3Client;
    private final R2Config r2Config;

    public R2StorageService(S3Client s3Client, R2Config r2Config) {
        this.s3Client = s3Client;
        this.r2Config = r2Config;
    }

    public String promoteImage(String tempImageKey) {
        validateTempKey(tempImageKey);

        String finalKey = FINAL_PREFIX + tempImageKey.substring(TEMP_PREFIX.length());

        copyObject(tempImageKey, finalKey);

        try {
            deleteObject(tempImageKey);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to delete temp image '%s' after copy. Will be cleaned by future job.", tempImageKey);
        }

        return finalKey;
    }

    public String buildPublicUrl(String key) {
        return r2Config.publicBaseUrl() + "/" + key;
    }

    public void deletePromotionImageIfPresent(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return;
        try {
            deleteObject(imageKey);
        } catch (Exception e) {
            LOG.warnf(e, "Failed to delete image '%s' from R2. Manual cleanup may be needed.", imageKey);
        }
    }

    public void validateTempKey(String imageKey) {
        if (imageKey == null || !TEMP_KEY.matcher(imageKey).matches()) {
            throw new IllegalArgumentException("invalid temporary imageKey");
        }
    }

    public void putImportedImage(byte[] bytes, String contentType, String imageKey) {
        PutObjectRequest putRequest = PutObjectRequest.builder()
                .bucket(r2Config.bucket())
                .key(imageKey)
                .contentType(contentType)
                .contentLength((long) bytes.length)
                .build();

        s3Client.putObject(putRequest, RequestBody.fromBytes(bytes));
        LOG.infof("Uploaded imported image: %s (%d bytes, %s)", imageKey, bytes.length, contentType);
    }

    private void copyObject(String sourceKey, String destinationKey) {
        s3Client.copyObject(CopyObjectRequest.builder()
                .sourceBucket(r2Config.bucket())
                .sourceKey(sourceKey)
                .destinationBucket(r2Config.bucket())
                .destinationKey(destinationKey)
                .build());
    }

    void deleteObject(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder()
                .bucket(r2Config.bucket())
                .key(key)
                .build());
    }
}
