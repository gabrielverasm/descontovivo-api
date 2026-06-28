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
            sb.append(" and (lower(title) like ?").append(idx).append(" or lower(description) like ?").append(idx).append(")");
            params.add("%" + query.toLowerCase() + "%");
            idx++;
        }

        return find(sb.toString(), Sort.by("createdAt").descending(), params.toArray())
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
            sb.append(" and (lower(title) like ?").append(idx).append(" or lower(description) like ?").append(idx).append(")");
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

    public boolean existsDuplicate(String normalizedUrl, String normalizedDescription, LocalDate createdDate) {
        return count("normalizedUrl = ?1 and normalizedDescription = ?2 and createdDate = ?3",
                normalizedUrl, normalizedDescription, createdDate) > 0;
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
}
