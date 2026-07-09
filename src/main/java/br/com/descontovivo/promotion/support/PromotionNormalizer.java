package br.com.descontovivo.promotion.support;

import java.net.URI;
import java.util.Arrays;
import java.util.Locale;
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

    /**
     * Normaliza o título de uma promoção: trim, reduz espaços múltiplos,
     * converte para minúsculo (pt-BR) e coloca apenas a primeira letra em maiúsculo.
     *
     * Exemplo: "CAPACETE FECHADO PRO TORK" → "Capacete fechado pro tork"
     */
    public static String normalizeTitle(String title) {
        if (title == null || title.isBlank()) return title;

        String normalized = title.trim().replaceAll("\\s+", " ").toLowerCase(Locale.forLanguageTag("pt-BR"));

        for (int i = 0; i < normalized.length(); i++) {
            if (Character.isLetter(normalized.charAt(i))) {
                return normalized.substring(0, i)
                        + Character.toUpperCase(normalized.charAt(i))
                        + normalized.substring(i + 1);
            }
        }
        return normalized;
    }
}
