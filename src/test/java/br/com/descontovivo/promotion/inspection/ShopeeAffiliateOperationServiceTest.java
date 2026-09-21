package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

class ShopeeAffiliateOperationServiceTest {
    private HttpServer server;

    @AfterEach void stop() { if (server != null) server.stop(0); }

    private String start(int status, String response, AtomicReference<String> token,
                         AtomicReference<String> body) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/internal/v1/shopee/affiliate-operation", exchange -> {
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

    private ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest requestWithValidationId(long validationId) {
        return new ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest(
                "validation", null, 1, 20, null, null, validationId, null, List.of(), null, null, null, null);
    }

    @Test void postsAuthenticatedJsonIncludingValidationId() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String baseUrl = start(200, "{\"validatedReport\":{\"nodes\":[]}}", token, body);
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));

        JsonNode result = service.execute(requestWithValidationId(555));

        assertEquals("secret", token.get());
        assertTrue(body.get().contains("\"validationId\":555"));
        assertTrue(result.has("validatedReport"));
    }

    @Test void sendsZeroWhenValidationIdIsAbsentForOtherOperations() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String baseUrl = start(200, "{\"shopOfferV2\":{\"nodes\":[]}}", token, body);
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        var request = new ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest(
                "shops", null, 1, 20, null, null, null, null, List.of(), null, null, null, null);

        service.execute(request);

        assertTrue(body.get().contains("\"validationId\":0"));
    }

    @Test void sendsEmptyStringSentinelsForAbsentOptionalStringFields() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String baseUrl = start(200, "{\"listItemFeeds\":{\"feeds\":[]}}", token, body);
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        var request = new ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest(
                "feeds", null, 1, 20, null, null, null, null, List.of(), null, null, null, null);

        service.execute(request);

        // Map.entry(...) can't hold null, so absent optional String fields must serialize as
        // "" (never omitted, never a literal null) -- the importer's Pydantic model treats ""
        // the same as "not provided" for these fields.
        assertTrue(body.get().contains("\"scrollId\":\"\""));
        assertTrue(body.get().contains("\"feedMode\":\"\""));
        assertTrue(body.get().contains("\"datafeedId\":\"\""));
        assertTrue(body.get().contains("\"offset\":0"));
    }

    @Test void forwardsScrollIdFeedModeDatafeedIdAndOffsetWhenProvided() throws Exception {
        AtomicReference<String> token = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        String baseUrl = start(200, "{\"getItemFeedData\":{\"rows\":[]}}", token, body);
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));
        var request = new ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest(
                "feed_data", null, 1, 20, null, null, null, null, List.of(),
                "scroll-abc", "DELTA", "feed_FULL_2026-09-08", 100L);

        service.execute(request);

        assertTrue(body.get().contains("\"scrollId\":\"scroll-abc\""));
        assertTrue(body.get().contains("\"feedMode\":\"DELTA\""));
        assertTrue(body.get().contains("\"datafeedId\":\"feed_FULL_2026-09-08\""));
        assertTrue(body.get().contains("\"offset\":100"));
    }

    @Test void preservesControlledImporterError() throws Exception {
        String baseUrl = start(400,
                "{\"detail\":{\"code\":\"INVALID_OPERATION\",\"message\":\"Shopee validation ID is invalid\"}}",
                new AtomicReference<>(), new AtomicReference<>());
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> service.execute(requestWithValidationId(-1)));

        assertEquals("INVALID_OPERATION", error.code());
        assertEquals("Shopee validation ID is invalid", error.getMessage());
    }

    @Test void rejectsMissingInternalTokenWithoutNetworkCall() {
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(),
                "http://127.0.0.1:1", Optional.empty(), Duration.ofMillis(50), Duration.ofMillis(50));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> service.execute(requestWithValidationId(1)));

        assertEquals("IMPORTER_UNAVAILABLE", error.code());
        assertNull(server);
    }

    @Test void collapsesUnauthorizedAndServerErrorsToImporterUnavailable() throws Exception {
        String baseUrl = start(401, "{}", new AtomicReference<>(), new AtomicReference<>());
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(), baseUrl,
                Optional.of("secret"), Duration.ofSeconds(1), Duration.ofSeconds(1));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> service.execute(requestWithValidationId(1)));

        assertEquals("IMPORTER_UNAVAILABLE", error.code());
    }

    @Test void mapsUnreachableImporterToImporterUnavailable() {
        var service = new ShopeeAffiliateOperationService(new ObjectMapper(),
                "http://127.0.0.1:1", Optional.of("secret"),
                Duration.ofMillis(100), Duration.ofMillis(100));

        MarketplaceInspectionException error = assertThrows(
                MarketplaceInspectionException.class,
                () -> service.execute(requestWithValidationId(1)));

        assertEquals("IMPORTER_UNAVAILABLE", error.code());
    }
}
