package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.config.R2Config;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

class R2StorageServiceValidationTest {
    private final R2StorageService service = new R2StorageService(
            mock(S3Client.class), mock(R2Config.class));

    @Test void acceptsOnlyWellFormedTemporaryKeys() {
        assertDoesNotThrow(() -> service.validateTempKey(
                "temp/promotions/2026/07/123e4567-e89b-12d3-a456-426614174000.webp"));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateTempKey("promotions/2026/07/file.webp"));
        assertThrows(IllegalArgumentException.class,
                () -> service.validateTempKey("temp/promotions/../../secret.webp"));
    }
}
