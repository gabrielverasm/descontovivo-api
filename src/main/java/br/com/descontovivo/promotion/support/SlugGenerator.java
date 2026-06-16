package br.com.descontovivo.promotion.support;

import java.text.Normalizer;
import java.util.UUID;

public final class SlugGenerator {

    private SlugGenerator() {}

    public static String fromTitle(String title) {
        String base = normalize(title);
        return base.isEmpty() ? UUID.randomUUID().toString().substring(0, 8) : base;
    }

    public static String withSuffix(String slug) {
        return slug + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private static String normalize(String input) {
        String decomposed = Normalizer.normalize(input, Normalizer.Form.NFD);
        return decomposed
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "")
                .toLowerCase()
                .replaceAll("[^a-z0-9\\s-]", "")
                .trim()
                .replaceAll("\\s+", "-")
                .replaceAll("-+", "-");
    }
}
