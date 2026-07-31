package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.BadRequestException;

import java.text.Normalizer;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;

@ApplicationScoped
public class PromotionCategorySelectionService {

    public static final int MAX_CATEGORIES = 4;
    public static final int MAX_CATEGORY_LENGTH = 50;

    private final PromotionRepository promotionRepository;

    public PromotionCategorySelectionService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    /**
     * Validates a category selection and resolves names equivalent by case,
     * accents or repeated whitespace to the spelling already persisted.
     * Unknown valid names remain unchanged and are created with the promotion.
     */
    public LinkedHashSet<String> resolve(List<String> categories, String legacyCategory) {
        List<String> source = categories != null
                ? categories
                : legacyCategory == null ? List.of() : List.of(legacyCategory);

        var canonicalNames = new LinkedHashMap<String, String>();
        for (Object[] row : promotionRepository.listDistinctCategoriesWithCount()) {
            String name = (String) row[0];
            canonicalNames.putIfAbsent(comparisonKey(name), name);
        }

        var resolved = new LinkedHashSet<String>();
        var selectedKeys = new LinkedHashSet<String>();
        for (String category : source) {
            if (category == null) continue;
            if (category.isBlank()) {
                throw new BadRequestException("O nome da categoria não pode ser vazio");
            }

            String normalizedName = normalizeWhitespace(category);
            if (normalizedName.length() > MAX_CATEGORY_LENGTH) {
                throw new BadRequestException("O nome da categoria deve ter no máximo 50 caracteres");
            }

            String key = comparisonKey(normalizedName);
            if (!selectedKeys.add(key)) continue;
            resolved.add(canonicalNames.getOrDefault(key, normalizedName));
        }
        if (resolved.size() > MAX_CATEGORIES) {
            throw new BadRequestException("Selecione no máximo quatro categorias");
        }
        return resolved;
    }

    static String comparisonKey(String value) {
        String decomposed = Normalizer.normalize(normalizeWhitespace(value), Normalizer.Form.NFD);
        return decomposed.replaceAll("\\p{M}+", "").toLowerCase(Locale.ROOT);
    }

    private static String normalizeWhitespace(String value) {
        return value.trim().replaceAll("\\s+", " ");
    }
}
