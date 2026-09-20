package br.com.descontovivo.promotion.inspection;

import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

/**
 * Inspeção da Amazon sem scraping: só o que o próprio link informa (ASIN, link canônico com a tag
 * de afiliado). O resto vai em missingFields até a Creators API estar disponível.
 */
@ApplicationScoped
public class AmazonMarketplaceInspectionProvider implements MarketplaceInspectionProvider {
    static final String STORE_NAME = "Amazon";
    static final List<String> MISSING_FIELDS = List.of(
            "title", "currentPrice", "originalPrice", "remoteImageUrl", "sellerName", "soldBy",
            "deliveredBy", "salesCount", "productRating", "sellerRating", "category");
    static final String MANUAL_FIELDS_WARNING = "A Amazon não informa os demais dados pelo link; preencha-os manualmente";
    static final String UNRESOLVED_SHORT_LINK_WARNING =
            "Não foi possível resolver o link curto; cole o link completo do SiteStripe";
    private static final int DUPLICATE_LOOKUP_LIMIT = 3;

    private final AmazonLinkCanonicalizer canonicalizer;
    private final AmazonShortLinkResolver resolver;
    private final PromotionRepository promotions;

    public AmazonMarketplaceInspectionProvider(AmazonLinkCanonicalizer canonicalizer,
                                               AmazonShortLinkResolver resolver,
                                               PromotionRepository promotions) {
        this.canonicalizer = canonicalizer;
        this.resolver = resolver;
        this.promotions = promotions;
    }

    @Override
    public boolean supports(MarketplaceCode marketplace) { return marketplace == MarketplaceCode.AMAZON; }

    @Override
    public MarketplaceInspectionData inspect(String url) {
        boolean shortLink = resolver.isShortLink(url);
        String productLink = url;
        if (shortLink) {
            try {
                productLink = resolver.resolve(url);
            } catch (MarketplaceInspectionException e) {
                return withoutLink(url);
            }
        }
        var link = canonicalizer.canonicalize(productLink);
        if (link.isEmpty()) {
            if (shortLink) return withoutLink(url);
            throw new MarketplaceInspectionException("INVALID_URL", "O link não aponta para um produto da Amazon");
        }

        var warnings = new ArrayList<String>();
        warnings.add(MANUAL_FIELDS_WARNING);
        List<PromotionEntity> existing = promotions.findActiveByAmazonAsin(link.get().asin(), DUPLICATE_LOOKUP_LIMIT);
        if (!existing.isEmpty()) {
            String slugs = String.join(", ", existing.stream().map(PromotionEntity::getSlug).toList());
            warnings.add("Já existe promoção deste produto (ASIN " + link.get().asin() + "): " + slugs);
        }
        return data(url, link.get().url(), warnings);
    }

    /** Link curto que não resolveu (ou não leva a um produto): devolve o que dá e pede o link completo. */
    private MarketplaceInspectionData withoutLink(String url) {
        return data(url, null, List.of(UNRESOLVED_SHORT_LINK_WARNING, MANUAL_FIELDS_WARNING));
    }

    private MarketplaceInspectionData data(String inputUrl, String canonicalUrl, List<String> warnings) {
        return new MarketplaceInspectionData(
                MarketplaceCode.AMAZON, inputUrl, canonicalUrl, canonicalUrl, null, null, null, null,
                STORE_NAME, null, null, null, null, null, null, false, false, null,
                MISSING_FIELDS, warnings);
    }
}
