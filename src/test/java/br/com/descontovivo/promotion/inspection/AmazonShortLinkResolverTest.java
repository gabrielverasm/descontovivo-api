package br.com.descontovivo.promotion.inspection;

import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.net.InetSocketAddress;
import java.time.Duration;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

class AmazonShortLinkResolverTest {
    private HttpServer server;
    private final Map<String, AtomicInteger> hits = new ConcurrentHashMap<>();

    @AfterEach void stop() { if (server != null) server.stop(0); }

    /** 127.0.0.1 faz o papel do host curto; localhost, o do host final (que nunca deve ser requisitado). */
    private AmazonShortLinkResolver start(Map<String, String[]> routes, int maxHops, Duration timeout) throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/", exchange -> {
            String path = exchange.getRequestURI().getPath();
            hits.computeIfAbsent(path, k -> new AtomicInteger()).incrementAndGet();
            String[] route = routes.get(path);
            if (route == null) {
                exchange.sendResponseHeaders(404, -1);
            } else {
                if (route[1] != null) exchange.getResponseHeaders().add("Location", route[1]);
                exchange.sendResponseHeaders(Integer.parseInt(route[0]), -1);
            }
            exchange.close();
        });
        server.start();
        return new AmazonShortLinkResolver(Set.of("127.0.0.1"), Set.of("localhost"), Set.of("http"), maxHops, timeout);
    }

    private String base() { return "http://127.0.0.1:" + server.getAddress().getPort(); }
    private String finalBase() { return "http://localhost:" + server.getAddress().getPort(); }

    @Test void followsRedirectsAndNeverRequestsTheProductPage() throws Exception {
        var routes = new ConcurrentHashMap<String, String[]>();
        var resolver = start(routes, 6, Duration.ofSeconds(2));
        routes.put("/a", new String[]{"301", "/b"});
        routes.put("/b", new String[]{"302", finalBase() + "/dp/B0ABCDE123?tag=x"});

        String result = resolver.resolve(base() + "/a");

        assertEquals(finalBase() + "/dp/B0ABCDE123?tag=x", result);
        assertEquals(1, hits.get("/a").get());
        assertEquals(1, hits.get("/b").get());
        assertNull(hits.get("/dp/B0ABCDE123"));
    }

    @Test void returnsFinalHostUrlWithoutAnyRequest() throws Exception {
        var resolver = start(new ConcurrentHashMap<>(), 6, Duration.ofSeconds(2));
        String url = finalBase() + "/dp/B0ABCDE123";
        assertEquals(url, resolver.resolve(url));
        assertTrue(hits.isEmpty());
    }

    @Test void rejectsRedirectToDisallowedHost() throws Exception {
        var routes = new ConcurrentHashMap<String, String[]>();
        var resolver = start(routes, 6, Duration.ofSeconds(2));
        routes.put("/a", new String[]{"302", "http://evil.example/dp/B0ABCDE123"});
        var error = assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/a"));
        assertEquals("INSPECTION_FAILED", error.code());
    }

    @Test void rejectsRedirectToDisallowedScheme() throws Exception {
        var routes = new ConcurrentHashMap<String, String[]>();
        var resolver = start(routes, 6, Duration.ofSeconds(2));
        routes.put("/a", new String[]{"302", "ftp://localhost/dp/B0ABCDE123"});
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/a"));
    }

    @Test void stopsAfterMaxHops() throws Exception {
        var routes = new ConcurrentHashMap<String, String[]>();
        var resolver = start(routes, 6, Duration.ofSeconds(2));
        routes.put("/a", new String[]{"302", "/a"});
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/a"));
        assertEquals(6, hits.get("/a").get());
    }

    @Test void failsWhenShortLinkDoesNotRedirect() throws Exception {
        var routes = new ConcurrentHashMap<String, String[]>();
        var resolver = start(routes, 6, Duration.ofSeconds(2));
        routes.put("/ok", new String[]{"200", null});
        routes.put("/nolocation", new String[]{"302", null});
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/ok"));
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/nolocation"));
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(base() + "/missing"));
    }

    @Test void failsOnConnectionError() throws Exception {
        var resolver = start(new ConcurrentHashMap<>(), 6, Duration.ofSeconds(1));
        String url = base() + "/a";
        server.stop(0);
        var error = assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve(url));
        assertEquals("INSPECTION_FAILED", error.code());
    }

    @Test void identifiesShortLinkHosts() {
        var resolver = new AmazonShortLinkResolver();
        assertTrue(resolver.isShortLink("https://link.amazon/abc"));
        assertTrue(resolver.isShortLink("https://amzn.to/abc"));
        assertTrue(resolver.isShortLink("https://amzlinks.in/abc"));
        assertFalse(resolver.isShortLink("https://www.amazon.com.br/dp/B0ABCDE123"));
        assertFalse(resolver.isShortLink("https://amzn.to.evil.example/abc"));
        assertFalse(resolver.isShortLink("not a url"));
    }

    @Test void productionResolverRejectsPlainHttpShortLink() {
        var resolver = new AmazonShortLinkResolver();
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve("http://amzn.to/abc"));
        assertThrows(MarketplaceInspectionException.class, () -> resolver.resolve("https://evil.example/abc"));
    }
}
