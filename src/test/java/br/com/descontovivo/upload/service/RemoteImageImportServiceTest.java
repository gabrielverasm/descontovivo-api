package br.com.descontovivo.upload.service;

import br.com.descontovivo.upload.service.RemoteImageImportService.RemoteImageException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for RemoteImageImportService URL validation and SSRF protection.
 * Does NOT test actual HTTP download (that requires integration/QuarkusTest).
 */
class RemoteImageImportServiceTest {

    // We only test the validateUrlForDryRun logic and URL parsing here,
    // which does NOT require a live HttpClient or actual DNS resolution.

    private final RemoteImageImportService service = new RemoteImageImportService(
            null, null, null, 2097152, null
    );

    // --- URL format validation tests ---

    @Test
    void shouldRejectNullUrl() {
        var ex = assertThrows(RemoteImageException.class, () -> service.validateUrlForDryRun(null));
        assertTrue(ex.getMessage().contains("obrigatória"));
    }

    @Test
    void shouldRejectBlankUrl() {
        var ex = assertThrows(RemoteImageException.class, () -> service.validateUrlForDryRun("   "));
        assertTrue(ex.getMessage().contains("obrigatória"));
    }

    @Test
    void shouldRejectFtpProtocol() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.validateUrlForDryRun("ftp://files.example.com/image.jpg"));
        assertTrue(ex.getMessage().contains("Protocolo inválido"));
    }

    @Test
    void shouldRejectFileProtocol() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.validateUrlForDryRun("file:///etc/passwd"));
        assertTrue(ex.getMessage().contains("Protocolo inválido"));
    }

    @Test
    void shouldRejectDataProtocol() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.validateUrlForDryRun("data:image/png;base64,abc"));
        assertTrue(ex.getMessage().contains("Protocolo inválido") || ex.getMessage().contains("inválida"));
    }

    @Test
    void shouldRejectUrlWithoutHost() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.validateUrlForDryRun("http:///path/image.jpg"));
        assertTrue(ex.getMessage().contains("sem host"));
    }

    // --- SSRF blocked hosts tests ---

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost/image.jpg",
            "http://127.0.0.1/image.jpg",
            "http://0.0.0.0/image.jpg",
            "http://[::1]/image.jpg"
    })
    void shouldBlockLocalhostAndLoopback(String url) {
        var ex = assertThrows(RemoteImageException.class, () -> service.validateUrlForDryRun(url));
        assertTrue(ex.getMessage().contains("bloqueado") || ex.getMessage().contains("privado"),
                "Expected SSRF block message but got: " + ex.getMessage());
    }

    // --- Valid URL passes format validation ---
    // Note: validateUrlForDryRun will try DNS resolution which may fail for fictional domains
    // in unit tests. We test with a real-ish domain that resolves.

    @Test
    void shouldPassValidUrlWithRealDomain() {
        // google.com resolves reliably
        assertDoesNotThrow(() ->
                service.validateUrlForDryRun("https://www.google.com/image.jpg"));
    }

    @Test
    void shouldPassHttpsUrl() {
        assertDoesNotThrow(() ->
                service.validateUrlForDryRun("https://www.google.com/test.png"));
    }

    @Test
    void shouldPassHttpUrl() {
        assertDoesNotThrow(() ->
                service.validateUrlForDryRun("http://www.google.com/test.webp"));
    }

    // --- importImage also validates ---

    @Test
    void shouldImportRejectNullUrl() {
        var ex = assertThrows(RemoteImageException.class, () -> service.importImage(null));
        assertTrue(ex.getMessage().contains("obrigatória"));
    }

    @Test
    void shouldImportRejectLocalhostUrl() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.importImage("http://localhost:9200/internal.jpg"));
        assertTrue(ex.getMessage().contains("bloqueado"));
    }

    @Test
    void shouldImportRejectInvalidProtocol() {
        var ex = assertThrows(RemoteImageException.class, () ->
                service.importImage("ftp://evil.com/exploit.jpg"));
        assertTrue(ex.getMessage().contains("Protocolo inválido"));
    }
}
