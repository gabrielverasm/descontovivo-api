package br.com.descontovivo.promotion.inspection;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class MarketplaceDetectorTest {
    private final MarketplaceDetector detector = new MarketplaceDetector();

    @Test void detectsKnownExactHosts() {
        assertEquals(MarketplaceCode.SHOPEE, detector.detect("https://s.shopee.com.br/x").orElseThrow());
        assertEquals(MarketplaceCode.AMAZON, detector.detect("https://amazon.com.br/x").orElseThrow());
        assertEquals(MarketplaceCode.MERCADO_LIVRE, detector.detect("https://meli.la/x").orElseThrow());
        assertEquals(MarketplaceCode.MAGALU, detector.detect("https://mglu.io/x").orElseThrow());
        assertEquals(MarketplaceCode.ALIEXPRESS, detector.detect("https://pt.aliexpress.com/x").orElseThrow());
    }

    @Test void rejectsLookalikeAndInvalidHosts() {
        assertTrue(detector.detect("https://shopee.com.br.evil.example/x").isEmpty());
        assertTrue(detector.detect("http://shopee.com.br/x").isEmpty());
        assertTrue(detector.detect("not-a-url").isEmpty());
    }
}
