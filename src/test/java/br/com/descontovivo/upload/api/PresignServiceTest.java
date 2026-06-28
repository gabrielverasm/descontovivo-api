package br.com.descontovivo.upload.api;

import br.com.descontovivo.upload.config.R2Config;
import br.com.descontovivo.upload.service.PresignService;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

class PresignServiceTest {

    @Test
    void shouldGenerateObjectKeyWithTempPrefix() throws Exception {
        // Use reflection to test the private buildObjectKey method directly
        R2Config config = new R2Config() {
            public String endpoint() { return "http://localhost:9000"; }
            public String region() { return "auto"; }
            public String accessKeyId() { return "test"; }
            public String secretAccessKey() { return "test"; }
            public String bucket() { return "test-bucket"; }
            public String publicBaseUrl() { return "https://img.descontovivo.com.br"; }
        };

        PresignService service = new PresignService(null, config, 300, 2097152, List.of("image/webp"));

        Method buildObjectKey = PresignService.class.getDeclaredMethod("buildObjectKey");
        buildObjectKey.setAccessible(true);
        String objectKey = (String) buildObjectKey.invoke(service);

        assertTrue(objectKey.startsWith("temp/promotions/"),
                "objectKey should start with temp/promotions/ but was: " + objectKey);
        assertTrue(objectKey.endsWith(".webp"));
        assertTrue(objectKey.matches("temp/promotions/\\d{4}/\\d{2}/[a-f0-9\\-]+\\.webp"),
                "objectKey should match pattern temp/promotions/yyyy/MM/uuid.webp but was: " + objectKey);
    }
}
