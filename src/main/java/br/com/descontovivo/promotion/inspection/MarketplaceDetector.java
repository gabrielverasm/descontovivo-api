package br.com.descontovivo.promotion.inspection;

import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@ApplicationScoped
public class MarketplaceDetector {
    private static final Map<MarketplaceCode, Set<String>> HOSTS = Map.of(
            MarketplaceCode.SHOPEE, Set.of("shopee.com.br", "www.shopee.com.br", "s.shopee.com.br"),
            MarketplaceCode.AMAZON, Set.of("amazon.com.br", "www.amazon.com.br", "amzn.to"),
            MarketplaceCode.MERCADO_LIVRE, Set.of("mercadolivre.com.br", "www.mercadolivre.com.br", "produto.mercadolivre.com.br", "meli.la"),
            MarketplaceCode.MAGALU, Set.of("magazineluiza.com.br", "www.magazineluiza.com.br", "mglu.io"),
            MarketplaceCode.ALIEXPRESS, Set.of("aliexpress.com", "www.aliexpress.com", "pt.aliexpress.com", "s.click.aliexpress.com")
    );

    public Optional<MarketplaceCode> detect(String value) {
        try {
            URI uri = URI.create(value == null ? "" : value.strip());
            if (!"https".equalsIgnoreCase(uri.getScheme()) || uri.getHost() == null) return Optional.empty();
            String host = uri.getHost().toLowerCase();
            return HOSTS.entrySet().stream()
                    .filter(entry -> entry.getValue().contains(host))
                    .map(Map.Entry::getKey)
                    .findFirst();
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
