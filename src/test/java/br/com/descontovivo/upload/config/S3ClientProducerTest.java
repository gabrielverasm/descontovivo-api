package br.com.descontovivo.upload.config;

import org.junit.jupiter.api.Test;
import software.amazon.awssdk.services.s3.S3Client;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class S3ClientProducerTest {

    @Test
    void s3Client_doesNotThrowDuplicatePathStyleConfig() {
        R2Config config = new R2Config() {
            @Override public String endpoint() { return "https://fake.r2.cloudflarestorage.com"; }
            @Override public String region() { return "auto"; }
            @Override public String accessKeyId() { return "fakeKey"; }
            @Override public String secretAccessKey() { return "fakeSecret"; }
            @Override public String bucket() { return "test-bucket"; }
            @Override public String publicBaseUrl() { return "https://img.test.com"; }
        };

        S3ClientProducer producer = new S3ClientProducer();

        S3Client client = assertDoesNotThrow(() -> producer.s3Client(config));
        assertNotNull(client);
        client.close();
    }
}
