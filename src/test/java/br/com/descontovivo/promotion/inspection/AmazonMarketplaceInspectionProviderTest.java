package br.com.descontovivo.promotion.inspection;

import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AmazonMarketplaceInspectionProviderTest {
    private static final String CANONICAL = "https://www.amazon.com.br/dp/B0ABCDE123?tag=descontovivoo-20";
    private final AmazonLinkCanonicalizer canonicalizer = new AmazonLinkCanonicalizer("descontovivoo-20");
    private final AmazonShortLinkResolver resolver = mock(AmazonShortLinkResolver.class);
    private final PromotionRepository promotions = mock(PromotionRepository.class);
    private final AmazonMarketplaceInspectionProvider provider =
            new AmazonMarketplaceInspectionProvider(canonicalizer, resolver, promotions);

    @Test void supportsOnlyAmazon() {
        assertTrue(provider.supports(MarketplaceCode.AMAZON));
        assertFalse(provider.supports(MarketplaceCode.SHOPEE));
    }

    @Test void returnsCanonicalLinkStoreAndMissingFields() {
        when(promotions.findActiveByAmazonAsin(anyString(), anyInt())).thenReturn(List.of());
        String input = "https://www.amazon.com.br/Produto/dp/B0ABCDE123/ref=x?tag=gatry0b-20";

        MarketplaceInspectionData data = provider.inspect(input);

        assertEquals(MarketplaceCode.AMAZON, data.marketplace());
        assertEquals(input, data.inputUrl());
        assertEquals(CANONICAL, data.productUrl());
        assertEquals(CANONICAL, data.affiliateUrl());
        assertEquals("Amazon", data.storeName());
        assertNull(data.title());
        assertNull(data.currentPrice());
        assertEquals(AmazonMarketplaceInspectionProvider.MISSING_FIELDS, data.missingFields());
        assertFalse(data.missingFields().contains("storeName"));
        assertEquals(1, data.warnings().size());
        verify(resolver, never()).resolve(anyString());
    }

    @Test void resolvesShortLinksBeforeCanonicalizing() {
        when(promotions.findActiveByAmazonAsin(anyString(), anyInt())).thenReturn(List.of());
        when(resolver.isShortLink("https://amzn.to/abc")).thenReturn(true);
        when(resolver.resolve("https://amzn.to/abc")).thenReturn("https://www.amazon.com.br/dp/B0ABCDE123?th=1");

        MarketplaceInspectionData data = provider.inspect("https://amzn.to/abc");

        assertEquals("https://www.amazon.com.br/dp/B0ABCDE123?tag=descontovivoo-20&th=1", data.productUrl());
        assertEquals("https://amzn.to/abc", data.inputUrl());
    }

    @Test void warnsAboutDuplicatesByAsin() {
        var existing = mock(PromotionEntity.class);
        when(existing.getSlug()).thenReturn("fone-bluetooth");
        when(promotions.findActiveByAmazonAsin("B0ABCDE123", 3)).thenReturn(List.of(existing));

        MarketplaceInspectionData data = provider.inspect("https://www.amazon.com.br/dp/B0ABCDE123");

        assertTrue(data.warnings().stream().anyMatch(w -> w.contains("B0ABCDE123") && w.contains("fone-bluetooth")));
    }

    @Test void rejectsLinkWithoutAsin() {
        var error = assertThrows(MarketplaceInspectionException.class,
                () -> provider.inspect("https://www.amazon.com.br/s?k=fone"));
        assertEquals("INVALID_URL", error.code());
        verifyNoInteractions(promotions);
    }

    @Test void unresolvedShortLinkReturnsPartialDataAndSiteStripeWarningInsteadOfFailing() {
        when(resolver.isShortLink("https://amzn.to/abc")).thenReturn(true);
        when(resolver.resolve("https://amzn.to/abc"))
                .thenThrow(new MarketplaceInspectionException("INSPECTION_FAILED", "timeout"));

        MarketplaceInspectionData data = provider.inspect("https://amzn.to/abc");

        assertEquals(MarketplaceCode.AMAZON, data.marketplace());
        assertEquals("https://amzn.to/abc", data.inputUrl());
        assertEquals("Amazon", data.storeName());
        assertNull(data.productUrl());
        assertNull(data.affiliateUrl());
        assertEquals(AmazonMarketplaceInspectionProvider.MISSING_FIELDS, data.missingFields());
        assertTrue(data.warnings().contains(AmazonMarketplaceInspectionProvider.UNRESOLVED_SHORT_LINK_WARNING));
        assertTrue(data.warnings().get(0).contains("SiteStripe"));
        verifyNoInteractions(promotions);
    }

    @Test void shortLinkResolvingToNonProductPageAlsoAsksForFullLink() {
        when(resolver.isShortLink("https://link.amazon/abc")).thenReturn(true);
        when(resolver.resolve("https://link.amazon/abc")).thenReturn("https://www.amazon.com.br/s?k=fone");

        MarketplaceInspectionData data = provider.inspect("https://link.amazon/abc");

        assertNull(data.productUrl());
        assertTrue(data.warnings().contains(AmazonMarketplaceInspectionProvider.UNRESOLVED_SHORT_LINK_WARNING));
    }

    @Test void linkWithAsinNeverDependsOnTheNetwork() {
        when(promotions.findActiveByAmazonAsin(anyString(), anyInt())).thenReturn(List.of());
        when(resolver.resolve(anyString())).thenThrow(new AssertionError("network must not be used"));

        MarketplaceInspectionData data = provider.inspect("https://www.amazon.com.br/dp/B0ABCDE123");

        assertEquals(CANONICAL, data.productUrl());
        verify(resolver, never()).resolve(anyString());
    }
}
