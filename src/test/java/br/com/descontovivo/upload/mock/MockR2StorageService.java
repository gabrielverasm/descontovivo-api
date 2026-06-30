package br.com.descontovivo.upload.mock;

import br.com.descontovivo.upload.service.R2StorageService;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Mock
@ApplicationScoped
public class MockR2StorageService extends R2StorageService {

    private final List<String> deletedKeys = new ArrayList<>();
    private final Map<String, byte[]> uploadedImages = new ConcurrentHashMap<>();
    private boolean shouldFailOnDelete = false;
    private boolean shouldFailOnUpload = false;

    public MockR2StorageService() {
        super(null, null);
    }

    @Override
    public String promoteImage(String tempImageKey) {
        validateTempKey(tempImageKey);
        return "promotions/" + tempImageKey.substring("temp/promotions/".length());
    }

    @Override
    public String buildPublicUrl(String key) {
        return "https://img.descontovivo.com.br/" + key;
    }

    @Override
    public void validateTempKey(String imageKey) {
        if (imageKey == null || !imageKey.startsWith("temp/promotions/")) {
            throw new IllegalArgumentException("imageKey must start with 'temp/promotions/'");
        }
    }

    @Override
    public void deletePromotionImageIfPresent(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return;
        if (shouldFailOnDelete) {
            // Simulates internal failure: warning logged, no exception propagated (mirrors real behavior)
            return;
        }
        deletedKeys.add(imageKey);
    }

    @Override
    public void putImportedImage(byte[] bytes, String contentType, String imageKey) {
        if (shouldFailOnUpload) {
            throw new RuntimeException("Simulated R2 upload failure");
        }
        uploadedImages.put(imageKey, bytes);
    }

    public Map<String, byte[]> getUploadedImages() {
        return uploadedImages;
    }

    public List<String> getDeletedKeys() {
        return deletedKeys;
    }

    public void clearDeletedKeys() {
        deletedKeys.clear();
    }

    public void clearUploadedImages() {
        uploadedImages.clear();
    }

    public void setShouldFailOnDelete(boolean shouldFail) {
        this.shouldFailOnDelete = shouldFail;
    }

    public void setShouldFailOnUpload(boolean shouldFail) {
        this.shouldFailOnUpload = shouldFail;
    }
}
