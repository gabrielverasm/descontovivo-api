package br.com.descontovivo.promotion.inspection;

import br.com.descontovivo.upload.service.RemoteImageImportService;
import br.com.descontovivo.upload.service.RemoteImageImportService.ImportedImage;
import br.com.descontovivo.upload.service.RemoteImageImportService.RemoteImageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import br.com.descontovivo.promotion.support.TrustSignalsHelper;
import org.jboss.logging.Logger;

import java.util.ArrayList;

@ApplicationScoped
public class PromotionInspectionService {
    private static final Logger LOG = Logger.getLogger(PromotionInspectionService.class);
    private final MarketplaceDetector detector;
    private final Instance<MarketplaceInspectionProvider> providers;
    private final RemoteImageImportService imageImporter;

    public PromotionInspectionService(MarketplaceDetector detector,
                                      Instance<MarketplaceInspectionProvider> providers,
                                      RemoteImageImportService imageImporter) {
        this.detector = detector;
        this.providers = providers;
        this.imageImporter = imageImporter;
    }

    public PromotionInspectionResponse inspect(String url) {
        MarketplaceCode marketplace = detector.detect(url)
                .orElseThrow(() -> new MarketplaceInspectionException("INVALID_URL", "URL de marketplace inválida"));
        MarketplaceInspectionProvider provider = providers.stream()
                .filter(candidate -> candidate.supports(marketplace)).findFirst()
                .orElseThrow(() -> new MarketplaceInspectionException("UNSUPPORTED_MARKETPLACE", "Integração ainda não disponível"));
        MarketplaceInspectionData data = provider.inspect(url);
        String imageKey = null;
        String imageUrl = null;
        var warnings = new ArrayList<>(data.warnings() == null ? java.util.List.<String>of() : data.warnings());
        if (data.remoteImageUrl() != null && !data.remoteImageUrl().isBlank()) {
            try {
                ImportedImage image = imageImporter.importImageToTemporaryStorage(data.remoteImageUrl());
                imageKey = image.imageKey();
                imageUrl = image.imageUrl();
            } catch (RemoteImageException e) {
                warnings.add("Falha ao importar imagem; revise-a manualmente");
            } catch (RuntimeException e) {
                LOG.error("Unexpected temporary image import failure; preserving inspection data", e);
                warnings.add("Falha ao importar imagem; revise-a manualmente");
            }
        }
        var baseSignals = new ArrayList<String>();
        if (data.shopeeGuarantee()) baseSignals.add("SHOPEE_GUARANTEE");
        var trustSignals = TrustSignalsHelper.deriveTrustSignals(
                marketplace.name(), data.salesCount() == null ? null : data.salesCount().intValue(),
                data.productRating() == null ? null : java.math.BigDecimal.valueOf(data.productRating()),
                data.sellerRating() == null ? null : java.math.BigDecimal.valueOf(data.sellerRating()),
                data.officialStore(), baseSignals);
        trustSignals.remove("CURATED_BY_DESCONTOVIVO");
        return new PromotionInspectionResponse(
                marketplace, true, data.inputUrl(), data.productUrl(), data.affiliateUrl(), data.title(),
                data.currentPrice(), data.originalPrice(), imageKey, imageUrl, data.storeName(),
                data.sellerName(), data.soldBy(), data.deliveredBy(), data.salesCount(),
                data.productRating(), data.sellerRating(), data.officialStore(), data.shopeeGuarantee(),
                data.category(), trustSignals,
                data.missingFields() == null ? java.util.List.of() : data.missingFields(), warnings);
    }
}
