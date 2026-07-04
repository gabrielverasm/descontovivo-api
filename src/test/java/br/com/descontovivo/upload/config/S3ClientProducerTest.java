package br.com.descontovivo.upload.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

import static org.junit.jupiter.api.Assertions.*;

class S3ClientProducerTest {

    private S3ClientProducer producer;
    private R2Config config;

    @BeforeEach
    void setUp() {
        producer = new S3ClientProducer();
        config = new R2Config() {
            @Override public String endpoint() { return "https://fake.r2.cloudflarestorage.com"; }
            @Override public String region() { return "auto"; }
            @Override public String accessKeyId() { return "fakeKey"; }
            @Override public String secretAccessKey() { return "fakeSecret"; }
            @Override public String bucket() { return "test-bucket"; }
            @Override public String publicBaseUrl() { return "https://img.test.com"; }
        };
    }

    @Test
    void s3Client_createsWithExplicitHttpClient() {
        S3Client client = assertDoesNotThrow(() -> producer.s3Client(config));
        assertNotNull(client);
        client.close();
    }

    @Test
    void s3Presigner_createsSuccessfully() {
        S3Presigner presigner = assertDoesNotThrow(() -> producer.s3Presigner(config));
        assertNotNull(presigner);
        presigner.close();
    }

    @Test
    void s3Client_doesNotExposeSecretsInExceptionMessage() {
        R2Config badConfig = new R2Config() {
            @Override public String endpoint() { return "not-a-valid-uri"; }
            @Override public String region() { return "auto"; }
            @Override public String accessKeyId() { return "sensitiveAccessKey123"; }
            @Override public String secretAccessKey() { return "sensitiveSecretKey456"; }
            @Override public String bucket() { return "test-bucket"; }
            @Override public String publicBaseUrl() { return "https://img.test.com"; }
        };

        try {
            producer.s3Client(badConfig);
            // If it doesn't throw, that's fine — the test is about exception content
        } catch (Exception e) {
            String message = e.getMessage() != null ? e.getMessage() : "";
            assertFalse(message.contains("sensitiveAccessKey123"),
                    "Exception message must not contain access key");
            assertFalse(message.contains("sensitiveSecretKey456"),
                    "Exception message must not contain secret key");
        }
    }

    @Test
    void s3Client_failsWithNullEndpoint() {
        R2Config nullEndpointConfig = new R2Config() {
            @Override public String endpoint() { return null; }
            @Override public String region() { return "auto"; }
            @Override public String accessKeyId() { return "fakeKey"; }
            @Override public String secretAccessKey() { return "fakeSecret"; }
            @Override public String bucket() { return "test-bucket"; }
            @Override public String publicBaseUrl() { return "https://img.test.com"; }
        };

        assertThrows(NullPointerException.class, () -> producer.s3Client(nullEndpointConfig));
    }

    @Test
    void s3Presigner_failsWithNullEndpoint() {
        R2Config nullEndpointConfig = new R2Config() {
            @Override public String endpoint() { return null; }
            @Override public String region() { return "auto"; }
            @Override public String accessKeyId() { return "fakeKey"; }
            @Override public String secretAccessKey() { return "fakeSecret"; }
            @Override public String bucket() { return "test-bucket"; }
            @Override public String publicBaseUrl() { return "https://img.test.com"; }
        };

        assertThrows(NullPointerException.class, () -> producer.s3Presigner(nullEndpointConfig));
    }
}
