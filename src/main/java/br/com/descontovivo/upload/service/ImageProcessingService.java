package br.com.descontovivo.upload.service;

import com.sksamuel.scrimage.ImmutableImage;
import com.sksamuel.scrimage.webp.WebpWriter;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;

@ApplicationScoped
public class ImageProcessingService {

    private static final Logger LOG = Logger.getLogger(ImageProcessingService.class);

    private final int targetSize;
    private final int webpQuality;

    public ImageProcessingService(
            @ConfigProperty(name = "image.target-size", defaultValue = "300") int targetSize,
            @ConfigProperty(name = "image.webp-quality", defaultValue = "75") int webpQuality
    ) {
        this.targetSize = targetSize;
        this.webpQuality = webpQuality;
    }

    public ProcessedImage process(byte[] originalBytes) {
        try {
            // Use ImageIO instead of scrimage ImageReaders to decode JPEG/PNG.
            // scrimage's ImageReaders relies on ServiceLoader which is broken in
            // GraalVM native image (empty registry at runtime).
            BufferedImage awt = ImageIO.read(new ByteArrayInputStream(originalBytes));
            if (awt == null) {
                throw new ImageProcessingException(
                        "Formato de imagem não suportado ou bytes inválidos. "
                                + "ImageIO não conseguiu decodificar os bytes fornecidos.", null);
            }

            ImmutableImage image = ImmutableImage.fromAwt(awt);

            int originalWidth = image.width;
            int originalHeight = image.height;

            // fit = contain: resize to fit inside targetSize x targetSize (preserving aspect ratio)
            // then pad with white background to reach exact target dimensions
            ImmutableImage processed = image.fit(targetSize, targetSize, Color.WHITE);

            byte[] webpBytes = processed.forWriter(WebpWriter.DEFAULT.withQ(webpQuality)).bytes();

            LOG.infof("Image processed: %dx%d -> %dx%d, %d bytes -> %d bytes (webp q%d)",
                    originalWidth, originalHeight, processed.width, processed.height,
                    originalBytes.length, webpBytes.length, webpQuality);

            return new ProcessedImage(webpBytes, "image/webp", "webp");

        } catch (ImageProcessingException e) {
            throw e;
        } catch (IOException e) {
            throw new ImageProcessingException("Falha ao processar imagem: " + e.getMessage(), e);
        }
    }

    public int getTargetSize() {
        return targetSize;
    }

    public record ProcessedImage(
            byte[] bytes,
            String contentType,
            String extension
    ) {}

    public static class ImageProcessingException extends RuntimeException {
        public ImageProcessingException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
