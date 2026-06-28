package br.com.descontovivo.upload.mock;

import br.com.descontovivo.upload.service.R2StorageService;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class MockR2StorageService extends R2StorageService {

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
}
