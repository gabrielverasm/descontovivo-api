package br.com.descontovivo.upload.mock;

import br.com.descontovivo.upload.service.R2StorageService;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@Mock
@ApplicationScoped
public class MockR2StorageService extends R2StorageService {

    private final List<String> deletedKeys = new ArrayList<>();
    private boolean shouldFailOnDelete = false;

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

    public List<String> getDeletedKeys() {
        return deletedKeys;
    }

    public void clearDeletedKeys() {
        deletedKeys.clear();
    }

    public void setShouldFailOnDelete(boolean shouldFail) {
        this.shouldFailOnDelete = shouldFail;
    }
}
