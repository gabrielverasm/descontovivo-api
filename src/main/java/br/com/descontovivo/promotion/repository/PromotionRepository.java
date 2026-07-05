package br.com.descontovivo.promotion.repository;

import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import io.quarkus.panache.common.Page;
import io.quarkus.panache.common.Sort;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class PromotionRepository implements PanacheRepositoryBase<PromotionEntity, UUID> {

    public Optional<PromotionEntity> findPublishedBySlug(String slug) {
        return find("slug = ?1 and status = ?2 and publishAt <= ?3", slug, PromotionStatus.PUBLISHED, OffsetDateTime.now()).firstResultOptional();
    }

    public List<PromotionEntity> listPublished(int page, int size, String storeSlug, String availability, String query) {
        var sb = new StringBuilder("status = ?1 and publishAt <= ?2");
        var params = new java.util.ArrayList<Object>();
        params.add(PromotionStatus.PUBLISHED);
        params.add(OffsetDateTime.now());
        int idx = 3;

        if (storeSlug != null && !storeSlug.isBlank()) {
            sb.append(" and store.slug = ?").append(idx++);
            params.add(storeSlug);
        }
        if (availability != null && !availability.isBlank()) {
            sb.append(" and availability = ?").append(idx++);
            params.add(br.com.descontovivo.promotion.entity.OfferAvailability.valueOf(availability));
        }
        if (query != null && !query.isBlank()) {
            sb.append(" and lower(title) like ?").append(idx);
            params.add("%" + query.toLowerCase() + "%");
            idx++;
        }

        return find(sb.toString(), Sort.by("publishAt").descending(), params.toArray())
                .page(Page.of(page, size))
                .list();
    }

    public long countPublished(String storeSlug, String availability, String query) {
        var sb = new StringBuilder("status = ?1 and publishAt <= ?2");
        var params = new java.util.ArrayList<Object>();
        params.add(PromotionStatus.PUBLISHED);
        params.add(OffsetDateTime.now());
        int idx = 3;

        if (storeSlug != null && !storeSlug.isBlank()) {
            sb.append(" and store.slug = ?").append(idx++);
            params.add(storeSlug);
        }
        if (availability != null && !availability.isBlank()) {
            sb.append(" and availability = ?").append(idx++);
            params.add(br.com.descontovivo.promotion.entity.OfferAvailability.valueOf(availability));
        }
        if (query != null && !query.isBlank()) {
            sb.append(" and lower(title) like ?").append(idx);
            params.add("%" + query.toLowerCase() + "%");
            idx++;
        }

        return count(sb.toString(), params.toArray());
    }

    public List<PromotionEntity> listByStatus(PromotionStatus status, int page, int size) {
        return find("status", Sort.by("createdAt").descending(), status)
                .page(Page.of(page, size))
                .list();
    }

    public boolean existsDuplicateByUrl(String normalizedUrl, LocalDate createdDate) {
        return count("normalizedUrl = ?1 and createdDate = ?2", normalizedUrl, createdDate) > 0;
    }

    public boolean existsBySourceId(String sourceId) {
        return count("sourceId", sourceId) > 0;
    }

    public boolean existsByNormalizedUrl(String normalizedUrl) {
        return count("normalizedUrl", normalizedUrl) > 0;
    }

    public List<PromotionEntity> findWithExternalImage(String r2BaseUrl, int limit) {
        return find("imageUrl is not null and imageUrl <> '' and imageUrl not like ?1",
                Sort.by("createdAt").ascending(), r2BaseUrl + "%")
                .page(Page.of(0, limit))
                .list();
    }

    /**
     * Count promotions currently visible (PUBLISHED and publishAt in the past).
     */
    public long countPublishedVisible() {
        return count("status = ?1 and publishAt <= ?2", PromotionStatus.PUBLISHED, OffsetDateTime.now());
    }

    /**
     * Find the publishedAt timestamp of the most recently published promotion.
     * Returns null if no published promotions exist.
     */
    public Optional<OffsetDateTime> findLatestPublishedAt() {
        return find("status = ?1 and publishAt <= ?2",
                Sort.by("publishedAt").descending(),
                PromotionStatus.PUBLISHED, OffsetDateTime.now())
                .page(Page.of(0, 1))
                .stream()
                .map(PromotionEntity::getPublishedAt)
                .findFirst();
    }

    /**
     * Count promotions pending moderation review.
     */
    public long countPendingModeration() {
        return count("status", PromotionStatus.PENDING_REVIEW);
    }

    /**
     * List distinct non-null/non-empty categories with promotion count, ordered alphabetically.
     */
    public List<Object[]> listDistinctCategoriesWithCount() {
        return getEntityManager()
                .createQuery(
                        "SELECT p.category, COUNT(p) FROM PromotionEntity p " +
                        "WHERE p.category IS NOT NULL AND LENGTH(TRIM(p.category)) > 0 " +
                        "GROUP BY p.category ORDER BY LOWER(p.category)",
                        Object[].class)
                .getResultList();
    }

    /**
     * Count promotions using an exact category name.
     */
    public long countByCategory(String category) {
        return count("category", category);
    }

    /**
     * Check if a category exists (exact match).
     */
    public boolean categoryExists(String category) {
        return count("category", category) > 0;
    }

    /**
     * Rename category on all promotions that use it.
     */
    public long renameCategory(String oldName, String newName) {
        return update("category = ?1 where category = ?2", newName, oldName);
    }

    /**
     * Clear category from all promotions using it (set to null).
     */
    public long clearCategory(String categoryName) {
        return update("category = null where category = ?1", categoryName);
    }
}
