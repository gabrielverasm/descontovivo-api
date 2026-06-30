package br.com.descontovivo.upload.mock;

import br.com.descontovivo.upload.service.ImageProcessingService;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

@Mock
@ApplicationScoped
public class MockImageProcessingService extends ImageProcessingService {

    private boolean shouldFail = false;

    public MockImageProcessingService() {
        super(300, 75);
    }

    @Override
    public ProcessedImage process(byte[] originalBytes) {
        if (shouldFail) {
            throw new ImageProcessingException("Simulated processing failure", new RuntimeException("test"));
        }

        // Return a minimal fake WebP byte array (RIFF + WEBP header)
        byte[] fakeWebp = new byte[64];
        fakeWebp[0] = 'R'; fakeWebp[1] = 'I'; fakeWebp[2] = 'F'; fakeWebp[3] = 'F';
        fakeWebp[8] = 'W'; fakeWebp[9] = 'E'; fakeWebp[10] = 'B'; fakeWebp[11] = 'P';

        return new ProcessedImage(fakeWebp, "image/webp", "webp");
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public void reset() {
        this.shouldFail = false;
    }
}
