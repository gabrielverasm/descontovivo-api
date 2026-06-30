package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.config.R2Config;
import br.com.descontovivo.upload.service.ImageProcessingService.ImageProcessingException;
import br.com.descontovivo.upload.service.ImageProcessingService.ProcessedImage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Set;
import java.util.UUID;

@ApplicationScoped
public class RemoteImageImportService {

    private static final Logger LOG = Logger.getLogger(RemoteImageImportService.class);
    private static final DateTimeFormatter YEAR_MONTH = DateTimeFormatter.ofPattern("yyyy/MM");

    private static final Set<String> BLOCKED_HOSTS = Set.of(
            "localhost", "127.0.0.1", "0.0.0.0", "::1",
            "[::1]", "metadata.google.internal", "169.254.169.254"
    );

    // Magic bytes for image format validation
    private static final byte[] JPEG_MAGIC = {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF};
    private static final byte[] PNG_MAGIC = {(byte) 0x89, 0x50, 0x4E, 0x47};
    private static final byte[] WEBP_RIFF = {0x52, 0x49, 0x46, 0x46}; // "RIFF"

    private final R2StorageService r2StorageService;
    private final ImageProcessingService imageProcessingService;
    private final R2Config r2Config;
    private final long maxFileSize;
    private final HttpClient httpClient;

    @Inject
    public RemoteImageImportService(
            R2StorageService r2StorageService,
            ImageProcessingService imageProcessingService,
            R2Config r2Config,
            @ConfigProperty(name = "upload.max-file-size") long maxFileSize
    ) {
        this.r2StorageService = r2StorageService;
        this.imageProcessingService = imageProcessingService;
        this.r2Config = r2Config;
        this.maxFileSize = maxFileSize;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    // Constructor for testing with injectable HttpClient (no image processing)
    protected RemoteImageImportService(
            R2StorageService r2StorageService,
            ImageProcessingService imageProcessingService,
            R2Config r2Config,
            long maxFileSize,
            HttpClient httpClient
    ) {
        this.r2StorageService = r2StorageService;
        this.imageProcessingService = imageProcessingService;
        this.r2Config = r2Config;
        this.maxFileSize = maxFileSize;
        this.httpClient = httpClient;
    }

    public ImportedImage importImage(String sourceUrl) {
        validateUrl(sourceUrl);

        URI uri = URI.create(sourceUrl);
        validateHost(uri);

        byte[] rawBytes = downloadImage(uri, sourceUrl);
        detectContentType(rawBytes, sourceUrl); // validate it's a real image

        // Process: resize to target size + convert to WebP
        ProcessedImage processed;
        try {
            processed = imageProcessingService.process(rawBytes);
        } catch (ImageProcessingException e) {
            throw new RemoteImageException("Falha ao processar imagem de " + sourceUrl + ": " + e.getMessage());
        }

        String imageKey = buildImageKey(processed.extension());
        String publicUrl = r2StorageService.buildPublicUrl(imageKey);

        r2StorageService.putImportedImage(processed.bytes(), processed.contentType(), imageKey);

        LOG.infof("Image imported: %s -> %s (original %d bytes, processed %d bytes, %s)",
                sourceUrl, imageKey, rawBytes.length, processed.bytes().length, processed.contentType());

        return new ImportedImage(imageKey, publicUrl, processed.contentType(), processed.bytes().length);
    }

    public void validateUrlForDryRun(String sourceUrl) {
        validateUrl(sourceUrl);
        URI uri = URI.create(sourceUrl);
        validateHost(uri);
    }

    // --- Validation ---

    private void validateUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new RemoteImageException("URL de imagem obrigatória");
        }

        URI uri;
        try {
            uri = URI.create(url.strip());
        } catch (IllegalArgumentException e) {
            throw new RemoteImageException("URL de imagem inválida: " + url);
        }

        String scheme = uri.getScheme();
        if (scheme == null || (!scheme.equals("http") && !scheme.equals("https"))) {
            throw new RemoteImageException("Protocolo inválido (apenas http/https permitido): " + url);
        }

        if (uri.getHost() == null || uri.getHost().isBlank()) {
            throw new RemoteImageException("URL sem host válido: " + url);
        }
    }

    private void validateHost(URI uri) {
        String host = uri.getHost().toLowerCase();

        if (BLOCKED_HOSTS.contains(host)) {
            throw new RemoteImageException("Domínio bloqueado (rede interna/localhost): " + host);
        }

        try {
            InetAddress[] addresses = InetAddress.getAllByName(host);
            for (InetAddress addr : addresses) {
                if (isPrivateOrReserved(addr)) {
                    throw new RemoteImageException("Domínio resolve para IP privado/reservado: " + host);
                }
            }
        } catch (UnknownHostException e) {
            throw new RemoteImageException("Não foi possível resolver o domínio: " + host);
        }
    }

    private boolean isPrivateOrReserved(InetAddress addr) {
        return addr.isLoopbackAddress()
                || addr.isSiteLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isMulticastAddress()
                || addr.isAnyLocalAddress();
    }

    // --- Download ---

    private byte[] downloadImage(URI uri, String sourceUrl) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(uri)
                .timeout(Duration.ofSeconds(10))
                .header("User-Agent", "DescontoVivo-ImageImporter/1.0")
                .header("Accept", "image/webp, image/jpeg, image/png, image/*")
                .GET()
                .build();

        HttpResponse<InputStream> response;
        try {
            response = httpClient.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new RemoteImageException("Erro ao baixar imagem: conexão falhou para " + sourceUrl);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RemoteImageException("Download de imagem interrompido: " + sourceUrl);
        }

        int statusCode = response.statusCode();
        if (statusCode < 200 || statusCode >= 300) {
            throw new RemoteImageException("Servidor retornou HTTP " + statusCode + " ao baixar imagem: " + sourceUrl);
        }

        // Check Content-Length header if present
        response.headers().firstValueAsLong("Content-Length").ifPresent(contentLength -> {
            if (contentLength > maxFileSize) {
                throw new RemoteImageException(
                        "Imagem maior que o limite permitido (" + maxFileSize + " bytes): " + sourceUrl);
            }
        });

        // Read body with size limit
        try (InputStream body = response.body()) {
            return readWithLimit(body, sourceUrl);
        } catch (IOException e) {
            if (e.getCause() instanceof RemoteImageException rie) throw rie;
            throw new RemoteImageException("Erro ao ler corpo da resposta de imagem: " + sourceUrl);
        }
    }

    private byte[] readWithLimit(InputStream input, String sourceUrl) throws IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        byte[] chunk = new byte[8192];
        long totalRead = 0;
        int bytesRead;

        while ((bytesRead = input.read(chunk)) != -1) {
            totalRead += bytesRead;
            if (totalRead > maxFileSize) {
                throw new RemoteImageException(
                        "Imagem maior que o limite permitido (" + maxFileSize + " bytes): " + sourceUrl);
            }
            buffer.write(chunk, 0, bytesRead);
        }

        if (totalRead == 0) {
            throw new RemoteImageException("Resposta sem conteúdo (0 bytes): " + sourceUrl);
        }

        return buffer.toByteArray();
    }

    // --- Content type detection via magic bytes ---

    private String detectContentType(byte[] bytes, String sourceUrl) {
        if (bytes.length < 12) {
            throw new RemoteImageException("Conteúdo muito pequeno para ser uma imagem válida: " + sourceUrl);
        }

        if (startsWith(bytes, JPEG_MAGIC)) return "image/jpeg";
        if (startsWith(bytes, PNG_MAGIC)) return "image/png";
        if (startsWith(bytes, WEBP_RIFF) && bytes.length > 11
                && bytes[8] == 'W' && bytes[9] == 'E' && bytes[10] == 'B' && bytes[11] == 'P') {
            return "image/webp";
        }

        throw new RemoteImageException("Conteúdo não é uma imagem válida (jpeg/png/webp): " + sourceUrl);
    }

    private static boolean startsWith(byte[] data, byte[] prefix) {
        if (data.length < prefix.length) return false;
        for (int i = 0; i < prefix.length; i++) {
            if (data[i] != prefix[i]) return false;
        }
        return true;
    }

    // --- Helpers ---

    private String buildImageKey(String extension) {
        String prefix = LocalDate.now().format(YEAR_MONTH);
        return "promotions/imported/" + prefix + "/" + UUID.randomUUID() + "." + extension;
    }

    // --- Value objects ---

    public record ImportedImage(
            String imageKey,
            String imageUrl,
            String contentType,
            long sizeBytes
    ) {}

    public static class RemoteImageException extends RuntimeException {
        public RemoteImageException(String message) {
            super(message);
        }
    }
}
