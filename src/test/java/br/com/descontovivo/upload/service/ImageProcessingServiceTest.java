package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.service.ImageProcessingService.ImageProcessingException;
import br.com.descontovivo.upload.service.ImageProcessingService.ProcessedImage;
import com.sksamuel.scrimage.ImmutableImage;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class ImageProcessingServiceTest {

    private final ImageProcessingService service = new ImageProcessingService(300, 75);

    @Test
    void shouldConvertJpegToWebp() throws IOException {
        byte[] jpegBytes = createTestJpeg(400, 300);

        ProcessedImage result = service.process(jpegBytes);

        assertEquals("image/webp", result.contentType());
        assertEquals("webp", result.extension());
        assertTrue(result.bytes().length > 0);
    }

    @Test
    void shouldConvertPngToWebp() throws IOException {
        byte[] pngBytes = createTestPng(500, 500);

        ProcessedImage result = service.process(pngBytes);

        assertEquals("image/webp", result.contentType());
        assertEquals("webp", result.extension());
    }

    @Test
    void shouldResizeLargeImageToTargetSize() throws IOException {
        byte[] largeJpeg = createTestJpeg(1200, 900);

        ProcessedImage result = service.process(largeJpeg);

        // Verify the output is valid and can be parsed back
        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width, "Width should be target size");
        assertEquals(300, output.height, "Height should be target size");
    }

    @Test
    void shouldPreserveAspectRatioWithContainBehavior() throws IOException {
        // Wide image: 1200x400 -> should fit to 300x100 then pad to 300x300
        byte[] wideJpeg = createTestJpeg(1200, 400);

        ProcessedImage result = service.process(wideJpeg);

        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width);
        assertEquals(300, output.height);
        // The product is not cropped — it's centered with white padding
    }

    @Test
    void shouldPreserveAspectRatioForTallImage() throws IOException {
        // Tall image: 400x1200 -> should fit to 100x300 then pad to 300x300
        byte[] tallJpeg = createTestJpeg(400, 1200);

        ProcessedImage result = service.process(tallJpeg);

        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width);
        assertEquals(300, output.height);
    }

    @Test
    void shouldHandleSmallImageWithoutUpscaling() throws IOException {
        // Image smaller than target: 100x100 -> fit pads to 300x300
        byte[] smallJpeg = createTestJpeg(100, 100);

        ProcessedImage result = service.process(smallJpeg);

        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width);
        assertEquals(300, output.height);
    }

    @Test
    void shouldHandleExactTargetSize() throws IOException {
        byte[] exactJpeg = createTestJpeg(300, 300);

        ProcessedImage result = service.process(exactJpeg);

        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width);
        assertEquals(300, output.height);
    }

    @Test
    void shouldProduceSmallerOutputThanOriginal() throws IOException {
        // Large JPEG should compress well to WebP
        byte[] largeJpeg = createTestJpeg(1200, 900);

        ProcessedImage result = service.process(largeJpeg);

        assertTrue(result.bytes().length < largeJpeg.length,
                "Processed image should be smaller than original large image");
    }

    @Test
    void shouldRejectInvalidImageBytes() {
        byte[] garbage = "this is not an image at all".getBytes();

        ImageProcessingException ex = assertThrows(ImageProcessingException.class, () -> service.process(garbage));
        assertTrue(ex.getMessage().contains("não suportado") || ex.getMessage().contains("inválidos"),
                "Error message should mention unsupported format or invalid bytes");
    }

    @Test
    void shouldRejectEmptyBytes() {
        byte[] empty = new byte[0];

        assertThrows(ImageProcessingException.class, () -> service.process(empty));
    }

    @Test
    void shouldUseConfiguredTargetSize() {
        assertEquals(300, service.getTargetSize());
    }

    @Test
    void shouldDecodeJpegViaImageIOPath() throws IOException {
        // Explicitly verify that the ImageIO-based path works for JPEG
        byte[] jpegBytes = createTestJpeg(600, 400);

        ProcessedImage result = service.process(jpegBytes);

        assertNotNull(result);
        assertEquals("image/webp", result.contentType());
        assertTrue(result.bytes().length > 0, "WebP output should have bytes");

        // Verify output dimensions via scrimage loader (works in JVM tests)
        ImmutableImage output = ImmutableImage.loader().fromBytes(result.bytes());
        assertEquals(300, output.width);
        assertEquals(300, output.height);
    }

    @Test
    void shouldDecodePngViaImageIOPath() throws IOException {
        // Explicitly verify that the ImageIO-based path works for PNG
        byte[] pngBytes = createTestPng(800, 600);

        ProcessedImage result = service.process(pngBytes);

        assertNotNull(result);
        assertEquals("image/webp", result.contentType());
        assertTrue(result.bytes().length > 0, "WebP output should have bytes");
    }

    // --- Helpers ---

    private byte[] createTestJpeg(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.RED);
        g.fillRect(0, 0, width / 2, height / 2);
        g.setColor(Color.BLUE);
        g.fillRect(width / 2, height / 2, width / 2, height / 2);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "JPEG", baos);
        return baos.toByteArray();
    }

    private byte[] createTestPng(int width, int height) throws IOException {
        BufferedImage img = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D g = img.createGraphics();
        g.setColor(Color.GREEN);
        g.fillOval(0, 0, width, height);
        g.dispose();

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(img, "PNG", baos);
        return baos.toByteArray();
    }
}
