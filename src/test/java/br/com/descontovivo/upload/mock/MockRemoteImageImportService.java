package br.com.descontovivo.upload.mock;

import br.com.descontovivo.upload.service.RemoteImageImportService;
import io.quarkus.test.Mock;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Mock
@ApplicationScoped
public class MockRemoteImageImportService extends RemoteImageImportService {

    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private final List<String> importedUrls = new ArrayList<>();
    private boolean shouldFail = false;
    private String failureMessage = "Simulated import failure";

    public MockRemoteImageImportService() {
        super(null, null, null, 2097152, null);
    }

    @Override
    public ImportedImage importImage(String sourceUrl) {
        if (shouldFail) {
            throw new RemoteImageException(failureMessage);
        }

        // Validate URL basic format (same as real service)
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RemoteImageException("URL de imagem obrigatória");
        }

        importedUrls.add(sourceUrl);

        String prefix = LocalDate.now().format(YEAR_MONTH);
        String imageKey = "promotions/imported/" + prefix + "/" + UUID.randomUUID() + ".webp";
        String publicUrl = "https://img.descontovivo.com.br/" + imageKey;

        return new ImportedImage(imageKey, publicUrl, "image/webp", 4096);
    }

    @Override
    public ImportedImage importImageToTemporaryStorage(String sourceUrl) {
        if (shouldFail) throw new RemoteImageException(failureMessage);
        importedUrls.add(sourceUrl);
        String key = "temp/promotions/" + LocalDate.now().format(YEAR_MONTH)
                + "/" + UUID.randomUUID() + ".webp";
        return new ImportedImage(key, "https://img.descontovivo.com.br/" + key, "image/webp", 4096);
    }

    @Override
    public void validateUrlForDryRun(String sourceUrl) {
        if (sourceUrl == null || sourceUrl.isBlank()) {
            throw new RemoteImageException("URL de imagem obrigatória");
        }

        // Simulate SSRF check for test URLs that should be blocked
        if (sourceUrl.contains("localhost") || sourceUrl.contains("127.0.0.1")
                || sourceUrl.contains("192.168.") || sourceUrl.contains("10.0.")) {
            throw new RemoteImageException("Domínio bloqueado (rede interna/localhost): " + sourceUrl);
        }
    }

    public List<String> getImportedUrls() {
        return importedUrls;
    }

    public void clearImportedUrls() {
        importedUrls.clear();
    }

    public void setShouldFail(boolean shouldFail) {
        this.shouldFail = shouldFail;
    }

    public void setFailureMessage(String message) {
        this.failureMessage = message;
    }

    public void reset() {
        importedUrls.clear();
        shouldFail = false;
        failureMessage = "Simulated import failure";
    }
}
