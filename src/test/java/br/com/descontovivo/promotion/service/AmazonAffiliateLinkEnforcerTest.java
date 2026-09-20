package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.inspection.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmazonAffiliateLinkEnforcerTest {
    private static final String CANONICAL = "https://www.amazon.com.br/dp/B0ABCDE123?tag=descontovivoo-20";
    private final AmazonShortLinkResolver resolver = mock(AmazonShortLinkResolver.class);

    private AmazonAffiliateLinkEnforcer enforcer(boolean enabled) {
        return new AmazonAffiliateLinkEnforcer(new MarketplaceDetector(),
                new AmazonLinkCanonicalizer("descontovivoo-20"), resolver, enabled);
    }

    @Test void disabledFlagLeavesEveryLinkUntouched() {
        var enforcer = enforcer(false);
        assertEquals("https://amzn.to/x", enforcer.enforce("https://amzn.to/x"));
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=gatry0b-20",
                enforcer.enforce("https://www.amazon.com.br/dp/B0ABCDE123?tag=gatry0b-20"));
        verifyNoInteractions(resolver);
    }

    @Test void nonAmazonAndBlankLinksPassThrough() {
        var enforcer = enforcer(true);
        assertEquals("https://shopee.com.br/x", enforcer.enforce("https://shopee.com.br/x"));
        assertEquals("https://example.com/amazon.com.br/dp/B0ABCDE123",
                enforcer.enforce("https://example.com/amazon.com.br/dp/B0ABCDE123"));
        assertNull(enforcer.enforce(null));
        assertEquals(" ", enforcer.enforce(" "));
    }

    @Test void directLinkIsCanonicalizedWithoutNetwork() {
        var enforcer = enforcer(true);
        assertEquals(CANONICAL, enforcer.enforce("https://www.amazon.com.br/Produto/dp/B0ABCDE123/ref=x?tag=descontoviv0f-20&utm_source=a"));
        assertEquals(CANONICAL, enforcer.enforce("http://amazon.com.br/dp/B0ABCDE123"));
        verify(resolver, never()).resolve(anyString());
    }

    @Test void shortLinkIsResolvedThenCanonicalized() {
        when(resolver.isShortLink("https://amzn.to/x")).thenReturn(true);
        when(resolver.resolve("https://amzn.to/x")).thenReturn("https://www.amazon.com.br/dp/B0ABCDE123?th=1");
        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=descontovivoo-20&th=1",
                enforcer(true).enforce("https://amzn.to/x"));
    }

    @Test void unresolvedShortLinkIsRejectedWithClearMessage() {
        when(resolver.isShortLink("https://link.amazon/x")).thenReturn(true);
        when(resolver.resolve("https://link.amazon/x"))
                .thenThrow(new MarketplaceInspectionException("INSPECTION_FAILED", "timeout"));
        var error = assertThrows(InvalidAmazonUrlException.class, () -> enforcer(true).enforce("https://link.amazon/x"));
        assertTrue(error.getMessage().contains("SiteStripe"));
    }

    @Test void amazonLinkWithoutProductIsRejected() {
        assertThrows(InvalidAmazonUrlException.class,
                () -> enforcer(true).enforce("https://www.amazon.com.br/s?k=fone"));
    }
}
