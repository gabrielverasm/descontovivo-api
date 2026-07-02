package br.com.descontovivo.promotion.support;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

public final class PromotionNormalizer {

    private static final Set<String> TRACKING_PARAMS = Set.of("utm_source", "utm_medium", "utm_campaign", "utm_term", "utm_content", "fbclid", "gclid");

    private PromotionNormalizer() {}

    public static String normalizeUrl(String url) {
        try {
            URI uri = URI.create(url.trim());
            String host = uri.getHost() != null ? uri.getHost().toLowerCase() : "";
            String path = uri.getPath() != null ? uri.getPath().replaceAll("/+$", "") : "";
            String query = uri.getQuery();
            String cleanQuery = "";
            if (query != null && !query.isBlank()) {
                cleanQuery = Arrays.stream(query.split("&"))
                        .filter(p -> {
                            String key = p.contains("=") ? p.substring(0, p.indexOf('=')) : p;
                            return !TRACKING_PARAMS.contains(key.toLowerCase());
                        })
                        .sorted()
                        .collect(Collectors.joining("&"));
            }
            String scheme = uri.getScheme() != null ? uri.getScheme().toLowerCase() : "https";
            return scheme + "://" + host + path + (cleanQuery.isEmpty() ? "" : "?" + cleanQuery);
        } catch (Exception e) {
            return url.trim().toLowerCase().replaceAll("/+$", "");
        }
    }
}
