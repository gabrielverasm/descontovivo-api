package br.com.descontovivo.store.service;

import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.store.entity.StoreEntity;
import br.com.descontovivo.store.repository.StoreRepository;
import jakarta.enterprise.context.ApplicationScoped;

import java.net.URI;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Centralizes store resolution: by name, by slug, by URL domain inference.
 * Creates stores on the fly when needed (find-or-create pattern).
 */
@ApplicationScoped
public class StoreResolver {

    private static final String FALLBACK_STORE_SLUG = "loja-nao-identificada";

    /** Names that should be treated as "no store provided" and skipped. */
    private static final Set<String> IGNORED_STORE_NAMES = Set.of(
            "loja não identificada",
            "loja nao identificada",
            "loja-nao-identificada"
    );

    /**
     * Maps known domains to their friendly display names.
     * The slug is generated automatically from the name via SlugGenerator.
     */
    private static final Map<String, String> DOMAIN_TO_STORE_NAME = Map.ofEntries(
            Map.entry("amazon.com.br", "Amazon"),
            Map.entry("amazon.com", "Amazon"),
            Map.entry("mercadolivre.com.br", "Mercado Livre"),
            Map.entry("magazineluiza.com.br", "Magazine Luiza"),
            Map.entry("magazinevoce.com.br", "Magazine Luiza"),
            Map.entry("magalu.com.br", "Magazine Luiza"),
            Map.entry("paguemenos.com.br", "Pague Menos"),
            Map.entry("paguemenos.com", "Pague Menos"),
            Map.entry("kabum.com.br", "KaBuM!"),
            Map.entry("aliexpress.com", "AliExpress"),
            Map.entry("pt.aliexpress.com", "AliExpress"),
            Map.entry("casasbahia.com.br", "Casas Bahia"),
            Map.entry("pontofrio.com.br", "Ponto Frio"),
            Map.entry("americanas.com.br", "Americanas"),
            Map.entry("shopee.com.br", "Shopee"),
            Map.entry("carrefour.com.br", "Carrefour"),
            Map.entry("extra.com.br", "Extra"),
            Map.entry("fastshop.com.br", "Fast Shop"),
            Map.entry("terabyteshop.com.br", "Terabyte Shop"),
            Map.entry("pichau.com.br", "Pichau")
    );

    private final StoreRepository storeRepository;

    public StoreResolver(StoreRepository storeRepository) {
        this.storeRepository = storeRepository;
    }

    /**
     * Resolves a store using the following priority:
     * 1. storeName (if provided and valid) → find-or-create by name
     * 2. storeSlug (if provided) → find by slug (throws 404 if not found)
     * 3. URL domain inference → find-or-create by inferred name
     * 4. Fallback → "Loja não identificada"
     */
    public StoreEntity resolve(String storeName, String storeSlug, String url) {
        // Priority 1: explicit name (sanitized)
        String sanitized = sanitizeStoreName(storeName);
        if (sanitized != null) {
            return findOrCreateByName(sanitized);
        }

        // Priority 2: explicit slug (backward compat)
        if (storeSlug != null && !storeSlug.isBlank()) {
            return storeRepository.findBySlug(storeSlug.trim())
                    .orElseThrow(() -> new jakarta.ws.rs.NotFoundException("Store not found: " + storeSlug.trim()));
        }

        // Priority 3: infer from URL
        Optional<String> inferredName = inferStoreNameFromUrl(url);
        if (inferredName.isPresent()) {
            return findOrCreateByName(inferredName.get());
        }

        // Priority 4: fallback
        return storeRepository.findBySlug(FALLBACK_STORE_SLUG)
                .orElseThrow(() -> new IllegalStateException("Fallback store not found"));
    }

    /**
     * Sanitizes storeName input:
     * - trims whitespace
     * - returns null if blank or matches a fallback/ignored name
     */
    private String sanitizeStoreName(String storeName) {
        if (storeName == null) return null;
        String trimmed = storeName.trim();
        if (trimmed.isEmpty()) return null;
        if (IGNORED_STORE_NAMES.contains(trimmed.toLowerCase())) return null;
        return trimmed;
    }

    /**
     * Find-or-create store by display name.
     * Generates slug internally from the name.
     * The name is stored trimmed.
     */
    public StoreEntity findOrCreateByName(String storeName) {
        String trimmed = storeName.trim();
        String slug = SlugGenerator.fromTitle(trimmed);
        Optional<StoreEntity> existing = storeRepository.findBySlug(slug);
        if (existing.isPresent()) return existing.get();

        var store = new StoreEntity();
        store.setName(trimmed);
        store.setSlug(slug);
        store.setCreatedAt(LocalDateTime.now());
        storeRepository.persist(store);
        return store;
    }

    /**
     * Infers a friendly store name from the URL domain.
     * Uses known domain map first, then applies generic domain-to-name logic.
     */
    public Optional<String> inferStoreNameFromUrl(String url) {
        if (url == null || url.isBlank()) return Optional.empty();
        try {
            String host = URI.create(url).getHost();
            if (host == null) return Optional.empty();
            host = host.toLowerCase().replaceFirst("^www\\.", "");

            // Check known domain map
            String knownName = DOMAIN_TO_STORE_NAME.get(host);
            if (knownName != null) {
                return Optional.of(knownName);
            }

            // Generic fallback: extract domain name and capitalize
            return Optional.of(friendlyNameFromDomain(host));
        } catch (Exception e) {
            return Optional.empty();
        }
    }

    /**
     * Converts a domain like "paguemenos.com.br" or "loja-exemplo.com" into a capitalized name.
     * Examples:
     *   "loja-exemplo.com.br" → "Loja Exemplo"
     *   "minha-loja.com" → "Minha Loja"
     *   "example.co.uk" → "Example"
     */
    private String friendlyNameFromDomain(String host) {
        // Remove common TLD suffixes
        String base = host
                .replaceAll("\\.(com\\.br|net\\.br|org\\.br|com|net|org|co\\.uk|io)$", "");

        // Remove any remaining dots (subdomains) – take last part
        if (base.contains(".")) {
            String[] parts = base.split("\\.");
            base = parts[parts.length - 1];
        }

        // Replace hyphens with spaces and capitalize each word
        String[] words = base.split("-");
        var sb = new StringBuilder();
        for (String word : words) {
            if (!word.isEmpty()) {
                if (sb.length() > 0) sb.append(' ');
                sb.append(Character.toUpperCase(word.charAt(0)));
                if (word.length() > 1) sb.append(word.substring(1));
            }
        }
        return sb.toString();
    }
}
