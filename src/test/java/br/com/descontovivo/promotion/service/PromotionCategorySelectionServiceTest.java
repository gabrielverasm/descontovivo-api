package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.ws.rs.BadRequestException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PromotionCategorySelectionServiceTest {

    private PromotionCategorySelectionService service;

    @BeforeEach
    void setUp() {
        PromotionRepository repository = mock(PromotionRepository.class);
        when(repository.listDistinctCategoriesWithCount()).thenReturn(List.of(
                new Object[] { "Saúde", 3L },
                new Object[] { "Casa e Jardim", 2L }
        ));
        service = new PromotionCategorySelectionService(repository);
    }

    @Test
    void resolvesExistingCanonicalNameAndPreservesNewName() {
        var categories = service.resolve(List.of("  SAUDE ", "Fitness   funcional"), null);

        assertEquals(List.of("Saúde", "Fitness funcional"), categories.stream().toList());
    }

    @Test
    void rejectsMoreThanFourCategories() {
        var error = assertThrows(BadRequestException.class,
                () -> service.resolve(List.of("A", "B", "C", "D", "E"), null));
        assertTrue(error.getMessage().contains("no máximo quatro"));
    }

    @Test
    void rejectsBlankAndOverlongNames() {
        assertThrows(BadRequestException.class, () -> service.resolve(List.of(" "), null));
        assertThrows(BadRequestException.class, () -> service.resolve(List.of("x".repeat(51)), null));
    }

    @Test
    void removesDuplicatesIgnoringCaseAccentsAndWhitespace() {
        var categories = service.resolve(java.util.Arrays.asList(
                null, "Saúde", "  SAUDE  ", "Casa   e Jardim", "CASA E JARDIM"), null);
        assertEquals(List.of("Saúde", "Casa e Jardim"), categories.stream().toList());
    }

    @Test
    void usesLegacyCategoryWhenTheListIsAbsent() {
        var categories = service.resolve(null, "  Fitness   funcional  ");

        assertEquals(List.of("Fitness funcional"), categories.stream().toList());
    }

    @Test
    void listTakesPrecedenceOverLegacyCategoryAndPreservesOrder() {
        var categories = service.resolve(List.of("Nova", "Saúde", "Casa e Jardim"), "Ignorada");

        assertEquals(List.of("Nova", "Saúde", "Casa e Jardim"), categories.stream().toList());
    }

    @Test
    void maximumIsAppliedAfterEquivalentDuplicatesAreRemoved() {
        var categories = service.resolve(
                List.of("Saúde", "SAUDE", "A", "B", "C"), null);

        assertEquals(List.of("Saúde", "A", "B", "C"), categories.stream().toList());
    }
}
