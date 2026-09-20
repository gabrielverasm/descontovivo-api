package br.com.descontovivo.promotion.inspection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

class AmazonLinkCanonicalizerTest {
    private static final String TAG = "descontovivoo-20";
    private final AmazonLinkCanonicalizer canonicalizer = new AmazonLinkCanonicalizer(TAG);

    private String canonical(String url) { return canonicalizer.canonicalize(url).orElseThrow().url(); }

    @Test void extractsAsinFromDpGpProductAndMobilePaths() {
        String expected = "https://www.amazon.com.br/dp/B0ABCDE123?tag=" + TAG;
        assertEquals(expected, canonical("https://www.amazon.com.br/dp/B0ABCDE123"));
        assertEquals(expected, canonical("https://www.amazon.com.br/Nome-Do-Produto/dp/B0ABCDE123/ref=sr_1_1?keywords=x"));
        assertEquals(expected, canonical("https://www.amazon.com.br/gp/product/B0ABCDE123?ref_=abc"));
        assertEquals(expected, canonical("https://amazon.com.br/gp/aw/d/B0ABCDE123/"));
        assertEquals("B0ABCDE123", canonicalizer.canonicalize("https://amazon.com.br/dp/b0abcde123").orElseThrow().asin());
    }

    @Test void keepsOnlyThPscAndSmidSortedAlphabeticallyWithTag() {
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?psc=1&smid=A1ZZFT5FULY4LN&tag=" + TAG + "&th=1",
                canonical("https://www.amazon.com.br/dp/B0ABCDE123?th=1&utm_source=x&smid=A1ZZFT5FULY4LN&fbclid=z&psc=1&keywords=a"));
    }

    @Test void dropsParamsWithUnsafeValues() {
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=" + TAG,
                canonical("https://www.amazon.com.br/dp/B0ABCDE123?th=1%22%3E&smid=&psc=a.b"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"descontoviv0f-20", "descontovivo.com-20", "gatry0b-20", "DESCONTOVIV0F-20", "outra-20", ""})
    void replacesAnyOtherTag(String tag) {
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=" + TAG,
                canonical("https://www.amazon.com.br/dp/B0ABCDE123?tag=" + tag));
    }

    @Test void keepsTheCurrentTagWithoutDuplicatingIt() {
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=" + TAG,
                canonical("https://www.amazon.com.br/dp/B0ABCDE123?tag=" + TAG + "&tag=other-20"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "https://www.amazon.com.br/s?k=fone",
            "https://www.amazon.com.br/",
            "https://www.amazon.com.br/dp/B0ABCDE12",
            "https://www.amazon.com.br/dp/B0ABCDE1234",
            "https://www.amazon.com.br/dp/B0ABCDE12!",
            "https://www.amazon.com.br/gp/offer-listing/B0ABCDE123",
            "https://www.amazon.com/dp/B0ABCDE123",
            "https://amazon.com.br.evil.example/dp/B0ABCDE123",
            "ftp://www.amazon.com.br/dp/B0ABCDE123",
            "not a url", ""})
    void rejectsLinksWithoutAsinOrForeignHosts(String url) {
        assertTrue(canonicalizer.canonicalize(url).isEmpty());
    }

    @Test void rejectsNull() { assertTrue(canonicalizer.canonicalize(null).isEmpty()); }

    @Test void refusesBlankOrRetiredConfiguredTag() {
        assertThrows(IllegalStateException.class, () -> new AmazonLinkCanonicalizer(" "));
        assertThrows(IllegalStateException.class, () -> new AmazonLinkCanonicalizer("gatry0b-20"));
    }
}
