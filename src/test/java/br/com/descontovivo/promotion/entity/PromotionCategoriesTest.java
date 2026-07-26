package br.com.descontovivo.promotion.entity;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashSet;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class PromotionCategoriesTest {

    @Test
    void synchronizesLegacyCategoryWithTheOrderedUniqueCollection() {
        var promotion = new PromotionEntity();

        promotion.setCategories(new LinkedHashSet<>(List.of("Casa", "Ofertas", "Casa")));

        assertEquals(List.of("Casa", "Ofertas"), List.copyOf(promotion.getCategories()));
        assertEquals("Casa", promotion.getCategory());

        promotion.setCategories(new LinkedHashSet<>());
        assertNull(promotion.getCategory());
    }
}
