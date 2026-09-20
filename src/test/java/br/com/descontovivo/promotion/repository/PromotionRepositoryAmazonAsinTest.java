package br.com.descontovivo.promotion.repository;

import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.store.entity.StoreEntity;
import br.com.descontovivo.store.repository.StoreRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class PromotionRepositoryAmazonAsinTest {
    @Inject PromotionRepository promotions;
    @Inject StoreRepository stores;

    private String create(String normalizedUrl, String sourceId, PromotionStatus status) {
        String slug = "asin-test-" + UUID.randomUUID().toString().substring(0, 8);
        QuarkusTransaction.requiringNew().run(() -> {
            var store = stores.findBySlug("asin-test-store").orElseGet(() -> {
                var created = new StoreEntity();
                created.setName("Asin Test Store");
                created.setSlug("asin-test-store");
                created.setCreatedAt(java.time.LocalDateTime.now());
                stores.persist(created);
                return created;
            });
            var now = OffsetDateTime.now();
            var entity = new PromotionEntity();
            entity.setSlug(slug);
            entity.setTitle("Asin test");
            entity.setUrl(normalizedUrl);
            entity.setNormalizedUrl(normalizedUrl);
            entity.setCurrentPrice(BigDecimal.TEN);
            entity.setImageUrl("https://img.example/a.webp");
            entity.setStatus(status);
            entity.setAvailability(OfferAvailability.AVAILABLE);
            entity.setStore(store);
            entity.setCreatedDate(LocalDate.now());
            entity.setCreatedAt(now);
            entity.setUpdatedAt(now);
            entity.setPublishAt(now);
            entity.setSourceId(sourceId);
            promotions.persist(entity);
        });
        return slug;
    }

    private List<String> slugs(String asin) {
        return QuarkusTransaction.requiringNew().call(() ->
                promotions.findActiveByAmazonAsin(asin, 10).stream().map(PromotionEntity::getSlug).toList());
    }

    @Test void matchesAllAmazonPathFormsAndSourceIdButOnlyActiveStatuses() {
        String asin = "B0TEST0001";
        String dp = create("https://www.amazon.com.br/dp/" + asin + "?tag=descontovivoo-20", null, PromotionStatus.PUBLISHED);
        String slugPath = create("https://www.amazon.com.br/nome-do-produto/dp/" + asin, null, PromotionStatus.PENDING_REVIEW);
        String gp = create("https://www.amazon.com.br/gp/product/" + asin.toLowerCase(), null, PromotionStatus.PUBLISHED);
        String bySource = create("https://amzn.to/xyz", asin, PromotionStatus.PUBLISHED);
        String rejected = create("https://www.amazon.com.br/dp/" + asin, null, PromotionStatus.REJECTED);
        String removed = create("https://www.amazon.com.br/dp/" + asin, null, PromotionStatus.REMOVED);
        String other = create("https://www.amazon.com.br/dp/B0TEST0002", null, PromotionStatus.PUBLISHED);

        List<String> found = slugs(asin);

        assertTrue(found.containsAll(List.of(dp, slugPath, gp, bySource)), found.toString());
        assertFalse(found.contains(rejected));
        assertFalse(found.contains(removed));
        assertFalse(found.contains(other));
    }

    @Test void respectsLimit() {
        String asin = "B0TEST0003";
        create("https://www.amazon.com.br/dp/" + asin, null, PromotionStatus.PUBLISHED);
        create("https://www.amazon.com.br/dp/" + asin + "?th=1", null, PromotionStatus.PUBLISHED);
        assertEquals(1, QuarkusTransaction.requiringNew().call(
                () -> promotions.findActiveByAmazonAsin(asin, 1).size()));
    }
}
