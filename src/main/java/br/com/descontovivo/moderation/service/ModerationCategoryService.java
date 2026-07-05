package br.com.descontovivo.moderation.service;

import br.com.descontovivo.moderation.api.CategoryResponse;
import br.com.descontovivo.moderation.api.RenameCategoryRequest;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.WebApplicationException;
import jakarta.ws.rs.core.Response;

import java.util.List;

@ApplicationScoped
public class ModerationCategoryService {

    private final PromotionRepository promotionRepository;

    public ModerationCategoryService(PromotionRepository promotionRepository) {
        this.promotionRepository = promotionRepository;
    }

    public List<CategoryResponse> listCategories() {
        return promotionRepository.listDistinctCategoriesWithCount()
                .stream()
                .map(row -> new CategoryResponse((String) row[0], ((Number) row[1]).longValue()))
                .toList();
    }

    @Transactional
    public CategoryResponse renameCategory(String oldName, RenameCategoryRequest request) {
        String newName = request.name().trim();

        if (newName.isEmpty()) {
            throw new BadRequestException("Nome da categoria não pode ser vazio");
        }

        if (!promotionRepository.categoryExists(oldName)) {
            throw new NotFoundException("Categoria não encontrada: " + oldName);
        }

        // Allow case/accent correction: if oldName and newName differ only in case, allow it
        if (!oldName.equals(newName) && promotionRepository.categoryExists(newName)) {
            throw new WebApplicationException(
                    Response.status(Response.Status.CONFLICT)
                            .entity(new br.com.descontovivo.shared.api.ApiErrorResponse(
                                    409, "Conflict", "Já existe uma categoria com o nome: " + newName))
                            .build());
        }

        long updated = promotionRepository.renameCategory(oldName, newName);
        return new CategoryResponse(newName, updated);
    }

    @Transactional
    public void deleteCategory(String categoryName) {
        if (!promotionRepository.categoryExists(categoryName)) {
            throw new NotFoundException("Categoria não encontrada: " + categoryName);
        }

        promotionRepository.clearCategory(categoryName);
    }
}
