package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.inspection.AmazonLinkCanonicalizer;
import br.com.descontovivo.promotion.inspection.AmazonShortLinkResolver;
import br.com.descontovivo.promotion.inspection.CanonicalAmazonLink;
import br.com.descontovivo.promotion.inspection.InvalidAmazonUrlException;
import br.com.descontovivo.promotion.inspection.MarketplaceCode;
import br.com.descontovivo.promotion.inspection.MarketplaceDetector;
import br.com.descontovivo.promotion.inspection.MarketplaceInspectionException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

/**
 * Garantia no servidor: com {@code app.amazon.enforce-tag=true}, todo link da Amazon gravado
 * (criação, edição e import) vira o link canônico com a tag de afiliado vigente. Links curtos são
 * resolvidos na hora; se não resolverem, o link é rejeitado. Links com ASIN nunca usam a rede.
 */
@ApplicationScoped
public class AmazonAffiliateLinkEnforcer {
    private final MarketplaceDetector detector;
    private final AmazonLinkCanonicalizer canonicalizer;
    private final AmazonShortLinkResolver resolver;
    private final boolean enabled;

    public AmazonAffiliateLinkEnforcer(MarketplaceDetector detector, AmazonLinkCanonicalizer canonicalizer,
                                       AmazonShortLinkResolver resolver,
                                       @ConfigProperty(name = "app.amazon.enforce-tag", defaultValue = "false") boolean enabled) {
        this.detector = detector;
        this.canonicalizer = canonicalizer;
        this.resolver = resolver;
        this.enabled = enabled;
    }

    /** Devolve o link a gravar; links que não são da Amazon (ou com a garantia desligada) passam intactos. */
    public String enforce(String url) {
        if (!enabled || url == null || url.isBlank()) return url;
        String candidate = url.strip().replaceFirst("(?i)^http://", "https://");
        if (detector.detect(candidate).orElse(null) != MarketplaceCode.AMAZON) return url;

        String productLink = candidate;
        if (resolver.isShortLink(candidate)) {
            try {
                productLink = resolver.resolve(candidate);
            } catch (MarketplaceInspectionException e) {
                throw new InvalidAmazonUrlException(
                        "Não foi possível resolver o link curto da Amazon; cole o link completo do SiteStripe");
            }
        }
        return canonicalizer.canonicalize(productLink)
                .map(CanonicalAmazonLink::url)
                .orElseThrow(() -> new InvalidAmazonUrlException(
                        "O link da Amazon não aponta para um produto; use o link do produto (…/dp/ASIN)"));
    }
}
