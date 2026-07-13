package br.com.descontovivo.promotion.inspection;

import br.com.descontovivo.upload.service.RemoteImageImportService;
import br.com.descontovivo.upload.service.RemoteImageImportService.ImportedImage;
import br.com.descontovivo.upload.service.RemoteImageImportService.RemoteImageException;
import jakarta.enterprise.inject.Instance;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PromotionInspectionServiceTest {
    @SuppressWarnings("unchecked")
    private PromotionInspectionService service(MarketplaceInspectionProvider provider,
                                               RemoteImageImportService images) {
        MarketplaceDetector detector = mock(MarketplaceDetector.class);
        when(detector.detect(anyString())).thenReturn(Optional.of(MarketplaceCode.SHOPEE));
        Instance<MarketplaceInspectionProvider> providers = mock(Instance.class);
        when(providers.stream()).thenReturn(Stream.of(provider));
        return new PromotionInspectionService(detector, providers, images);
    }

    private MarketplaceInspectionData data() {
        return new MarketplaceInspectionData(MarketplaceCode.SHOPEE, "input", "product", null,
                "Title", new BigDecimal("10"), null, "https://image.example/a.webp",
                null, "Seller", null, null, null, null, null, false, false,
                null, List.of("storeName"), List.of());
    }

    @Test void importsImageToTemporaryStorageAndPreservesIndependentSellerFields() {
        MarketplaceInspectionProvider provider = mock(MarketplaceInspectionProvider.class);
        when(provider.supports(MarketplaceCode.SHOPEE)).thenReturn(true);
        when(provider.inspect(anyString())).thenReturn(data());
        RemoteImageImportService images = mock(RemoteImageImportService.class);
        when(images.importImageToTemporaryStorage(anyString())).thenReturn(new ImportedImage(
                "temp/promotions/2026/07/id.webp", "https://img/temp/promotions/2026/07/id.webp",
                "image/webp", 100));

        PromotionInspectionResponse response = service(provider, images).inspect("https://shopee.com.br/x");

        assertTrue(response.imageKey().startsWith("temp/promotions/"));
        assertEquals("Seller", response.sellerName());
        assertNull(response.soldBy());
        assertNull(response.deliveredBy());
        assertFalse(response.trustSignals().contains("CURATED_BY_DESCONTOVIVO"));
        verify(images).importImageToTemporaryStorage("https://image.example/a.webp");
    }

    @Test void derivesShopeeGuaranteeAndOfficialStoreSignals() {
        MarketplaceInspectionData guaranteed = new MarketplaceInspectionData(
                MarketplaceCode.SHOPEE, "input", "product", null, "Title",
                new BigDecimal("10"), null, null, null, null, null, null,
                null, null, null, true, true, null, List.of(), List.of());
        MarketplaceInspectionProvider provider = mock(MarketplaceInspectionProvider.class);
        when(provider.supports(MarketplaceCode.SHOPEE)).thenReturn(true);
        when(provider.inspect(anyString())).thenReturn(guaranteed);
        PromotionInspectionResponse response = service(provider, mock(RemoteImageImportService.class))
                .inspect("https://shopee.com.br/x");
        assertTrue(response.trustSignals().contains("SHOPEE_GUARANTEE"));
        assertTrue(response.trustSignals().contains("OFFICIAL_STORE"));
        assertFalse(response.trustSignals().contains("CURATED_BY_DESCONTOVIVO"));
    }

    @Test void imageFailureKeepsProductDataAndAddsWarning() {
        MarketplaceInspectionProvider provider = mock(MarketplaceInspectionProvider.class);
        when(provider.supports(MarketplaceCode.SHOPEE)).thenReturn(true);
        when(provider.inspect(anyString())).thenReturn(data());
        RemoteImageImportService images = mock(RemoteImageImportService.class);
        when(images.importImageToTemporaryStorage(anyString())).thenThrow(new RemoteImageException("failed"));

        PromotionInspectionResponse response = service(provider, images).inspect("https://shopee.com.br/x");

        assertEquals("Title", response.title());
        assertNull(response.imageKey());
        assertFalse(response.warnings().isEmpty());
    }

    @Test void unexpectedImageStorageFailureKeepsProductDataAndAddsWarning() {
        MarketplaceInspectionProvider provider = mock(MarketplaceInspectionProvider.class);
        when(provider.supports(MarketplaceCode.SHOPEE)).thenReturn(true);
        when(provider.inspect(anyString())).thenReturn(data());
        RemoteImageImportService images = mock(RemoteImageImportService.class);
        when(images.importImageToTemporaryStorage(anyString()))
                .thenThrow(new IllegalStateException("storage unavailable"));

        PromotionInspectionResponse response = service(provider, images).inspect("https://shopee.com.br/x");

        assertEquals("Title", response.title());
        assertNull(response.imageKey());
        assertFalse(response.warnings().isEmpty());
    }
}
