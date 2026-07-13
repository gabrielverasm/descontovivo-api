package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ShopeeMarketplaceInspectionProviderTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    private String start(int status, String response, AtomicReference<String> token,
                         AtomicReference<String> body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/shopee/inspect", exchange -> {
            token.set(exchange.getRequestHeaders().getFirst("X-Internal-Token"));
            body.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            byte[] bytes = response.getBytes(StandardCharsets.UTF_8);
            exchange.sendResponseHeaders(status, bytes.length);
            exchange.getResponseBody().write(bytes);
            exchange.close();
        });
        server.start();
        return "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @Test void postsAuthenticatedJsonAndMapsPartialResponse() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String json = "{\"marketplace\":\"SHOPEE\",\"inputUrl\":\"in\",\"productUrl\":\"product\",\"officialStore\":false,\"shopeeGuarantee\":false,\"missingFields\":[],\"warnings\":[]}";
        String baseUrl = start(200, json, token, body);
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));

        MarketplaceInspectionData result = provider.inspect("https://shopee.com.br/x");

        assertEquals("secret", token.get());
        assertTrue(body.get().contains("https://shopee.com.br/x"));
        assertEquals(MarketplaceCode.SHOPEE, result.marketplace());
        assertEquals("product", result.productUrl());
    }

    @Test void preservesControlledImporterError() throws Exception {
        String baseUrl = start(422,
                "{\"detail\":{\"code\":\"INSPECTION_BLOCKED\",\"message\":\"CAPTCHA\"}}",
                new AtomicReference<>(), new AtomicReference<>());
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> provider.inspect("https://shopee.com.br/x"));
        assertEquals("INSPECTION_BLOCKED", error.code());
        assertEquals("CAPTCHA", error.getMessage());
    }

    @Test void rejectsMissingInternalTokenWithoutNetworkCall() {
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(),
                "http://127.0.0.1:1", Optional.empty(), Duration.ofMillis(50), Duration.ofMillis(50));
        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> provider.inspect("https://shopee.com.br/x"));
        assertEquals("IMPORTER_UNAVAILABLE", error.code());
        assertNull(server);
    }

    @Test void mapsUnauthorizedToUnavailable() throws Exception {
        String baseUrl = start(401, "{}", new AtomicReference<>(), new AtomicReference<>());
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(), baseUrl,
                Optional.of("bad"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> provider.inspect("https://shopee.com.br/x"));
        assertEquals("IMPORTER_UNAVAILABLE", error.code());
        assertTrue(error.getMessage().contains("authentication"));
    }

    @Test void mapsUnavailableImporterToServiceUnavailableError() {
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(),
                "http://127.0.0.1:1", Optional.of("secret"),
                Duration.ofMillis(100), Duration.ofMillis(100));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> provider.inspect("https://shopee.com.br/x"));

        assertEquals("IMPORTER_UNAVAILABLE", error.code());
    }

    @Test void mapsInvalidImporterJsonToServiceUnavailableError() throws Exception {
        String baseUrl = start(200, "not-json", new AtomicReference<>(), new AtomicReference<>());
        var provider = new ShopeeMarketplaceInspectionProvider(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> provider.inspect("https://shopee.com.br/x"));

        assertEquals("IMPORTER_UNAVAILABLE", error.code());
    }
}
