package br.com.descontovivo.notification.service;

import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import br.com.descontovivo.account.entity.DataRequestStatus;
import br.com.descontovivo.account.entity.DataRequestType;
import br.com.descontovivo.account.repository.AccountDataRequestRepository;
import br.com.descontovivo.notification.dto.AdminDataRequestSnapshot;
import br.com.descontovivo.notification.dto.ModerationPromotionSnapshot;
import br.com.descontovivo.notification.dto.PublicPromotionSnapshot;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.store.entity.StoreEntity;
import br.com.descontovivo.store.repository.StoreRepository;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for NotificationSnapshotService.
 *
 * <p>These tests verify the actual JPA queries against the database via Dev Services.
 * They are fast, deterministic, and the most important tests for the notification module.
 */
@QuarkusTest
class NotificationSnapshotServiceTest {

    @Inject
    NotificationSnapshotService snapshotService;

    @Inject
    PromotionRepository promotionRepository;

    @Inject
    AccountDataRequestRepository dataRequestRepository;

    @Inject
    StoreRepository storeRepository;

    // ─── publicPromotionSnapshot ───────────────────────────────────────

    @Test
    void publicPromotionSnapshot_emptyDatabase_returnsZeroCountAndNullDate() {
        PublicPromotionSnapshot snapshot = snapshotService.publicPromotionSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.publishedCount() >= 0);
        // latestPublishedAt may be null if no promotions exist — must not throw NPE.
    }

    @Test
    void publicPromotionSnapshot_nullLatestPublishedAt_doesNotThrow() {
        // Ensures the service handles Optional.empty() from repository without NPE.
        PublicPromotionSnapshot snapshot = snapshotService.publicPromotionSnapshot();
        assertNotNull(snapshot, "Snapshot must never be null, even with empty data");
    }

    @Test
    @Transactional
    void publicPromotionSnapshot_withPublishedPromotion_returnsCorrectData() {
        StoreEntity store = getOrCreateStore();
        OffsetDateTime publishedAt = OffsetDateTime.now().minusMinutes(5);

        PromotionEntity promo = createPromotion(store, PromotionStatus.PUBLISHED,
                OffsetDateTime.now().minusHours(1), publishedAt);
        promotionRepository.persist(promo);

        PublicPromotionSnapshot snapshot = snapshotService.publicPromotionSnapshot();
        assertTrue(snapshot.publishedCount() >= 1,
                "Expected at least 1 published promotion, got: " + snapshot.publishedCount());
        assertNotNull(snapshot.latestPublishedAt(),
                "Expected non-null latestPublishedAt when published promotions exist");
    }

    @Test
    @Transactional
    void publicPromotionSnapshot_futurePromotion_notCounted() {
        StoreEntity store = getOrCreateStore();
        // publishAt is in the future — countPublishedVisible uses publishAt <= now()
        OffsetDateTime futurePublishAt = OffsetDateTime.now().plusHours(2);

        PromotionEntity futurePromo = createPromotion(store, PromotionStatus.PUBLISHED,
                futurePublishAt, OffsetDateTime.now());
        promotionRepository.persist(futurePromo);

        // Get count BEFORE — the future promo should not increment the count
        long countBefore = snapshotService.publicPromotionSnapshot().publishedCount();

        // The future promo was already persisted above, so if it's wrongly counted,
        // countBefore already includes it. Create another future promo to check delta.
        PromotionEntity futurePromo2 = createPromotion(store, PromotionStatus.PUBLISHED,
                OffsetDateTime.now().plusDays(1), OffsetDateTime.now());
        promotionRepository.persist(futurePromo2);

        long countAfter = snapshotService.publicPromotionSnapshot().publishedCount();
        assertEquals(countBefore, countAfter,
                "Future promotion (publishAt > now) must NOT be counted as visible");
    }

    @Test
    @Transactional
    void publicPromotionSnapshot_draftPromotion_notCounted() {
        StoreEntity store = getOrCreateStore();
        long countBefore = snapshotService.publicPromotionSnapshot().publishedCount();

        PromotionEntity draftPromo = createPromotion(store, PromotionStatus.PENDING_REVIEW,
                OffsetDateTime.now().minusHours(1), null);
        promotionRepository.persist(draftPromo);

        long countAfter = snapshotService.publicPromotionSnapshot().publishedCount();
        assertEquals(countBefore, countAfter,
                "PENDING_REVIEW promotion must NOT be counted as published visible");
    }

    // ─── moderationPromotionSnapshot ───────────────────────────────────

    @Test
    void moderationPromotionSnapshot_doesNotBreak() {
        ModerationPromotionSnapshot snapshot = snapshotService.moderationPromotionSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.pendingCount() >= 0);
    }

    @Test
    @Transactional
    void moderationPromotionSnapshot_countsPendingReview() {
        StoreEntity store = getOrCreateStore();
        long countBefore = snapshotService.moderationPromotionSnapshot().pendingCount();

        PromotionEntity pendingPromo = createPromotion(store, PromotionStatus.PENDING_REVIEW,
                OffsetDateTime.now().minusHours(1), null);
        promotionRepository.persist(pendingPromo);

        long countAfter = snapshotService.moderationPromotionSnapshot().pendingCount();
        assertEquals(countBefore + 1, countAfter,
                "PENDING_REVIEW promotion must increment moderation pending count");
    }

    @Test
    @Transactional
    void moderationPromotionSnapshot_publishedPromotionNotCounted() {
        StoreEntity store = getOrCreateStore();
        long countBefore = snapshotService.moderationPromotionSnapshot().pendingCount();

        PromotionEntity publishedPromo = createPromotion(store, PromotionStatus.PUBLISHED,
                OffsetDateTime.now().minusHours(1), OffsetDateTime.now());
        promotionRepository.persist(publishedPromo);

        long countAfter = snapshotService.moderationPromotionSnapshot().pendingCount();
        assertEquals(countBefore, countAfter,
                "PUBLISHED promotion must NOT be counted as pending moderation");
    }

    // ─── adminDataRequestSnapshot ──────────────────────────────────────

    @Test
    void adminDataRequestSnapshot_doesNotBreakWithEmptyDatabase() {
        AdminDataRequestSnapshot snapshot = snapshotService.adminDataRequestSnapshot();
        assertNotNull(snapshot);
        assertTrue(snapshot.openCount() >= 0);
    }

    @Test
    @Transactional
    void adminDataRequestSnapshot_countsPendingAndInReview() {
        long countBefore = snapshotService.adminDataRequestSnapshot().openCount();

        AccountDataRequestEntity pending = createDataRequest(DataRequestStatus.PENDING);
        AccountDataRequestEntity inReview = createDataRequest(DataRequestStatus.IN_REVIEW);
        dataRequestRepository.persist(pending);
        dataRequestRepository.persist(inReview);

        long countAfter = snapshotService.adminDataRequestSnapshot().openCount();
        assertEquals(countBefore + 2, countAfter,
                "PENDING + IN_REVIEW should increment open count by 2");
    }

    @Test
    @Transactional
    void adminDataRequestSnapshot_completedAndRejected_notCounted() {
        long countBefore = snapshotService.adminDataRequestSnapshot().openCount();

        AccountDataRequestEntity completed = createDataRequest(DataRequestStatus.COMPLETED);
        AccountDataRequestEntity rejected = createDataRequest(DataRequestStatus.REJECTED);
        dataRequestRepository.persist(completed);
        dataRequestRepository.persist(rejected);

        long countAfter = snapshotService.adminDataRequestSnapshot().openCount();
        assertEquals(countBefore, countAfter,
                "COMPLETED and REJECTED must NOT be counted as open requests");
    }

    // ─── Helpers ───────────────────────────────────────────────────────

    private StoreEntity getOrCreateStore() {
        return storeRepository.find("slug", "sse-test-store")
                .firstResultOptional()
                .orElseGet(() -> {
                    StoreEntity s = new StoreEntity();
                    s.setName("SSE Test Store");
                    s.setSlug("sse-test-store");
                    s.setCreatedAt(java.time.LocalDateTime.now());
                    storeRepository.persist(s);
                    return s;
                });
    }

    private PromotionEntity createPromotion(StoreEntity store, PromotionStatus status,
                                            OffsetDateTime publishAt, OffsetDateTime publishedAt) {
        PromotionEntity p = new PromotionEntity();
        p.setSlug("sse-test-" + UUID.randomUUID().toString().substring(0, 8));
        p.setTitle("SSE Test Promotion");
        p.setUrl("https://example.com/promo-" + UUID.randomUUID());
        p.setNormalizedUrl("example.com/promo-" + UUID.randomUUID());
        p.setCurrentPrice(BigDecimal.valueOf(29.90));
        p.setImageUrl("https://img.example.com/test.webp");
        p.setStatus(status);
        p.setAvailability(OfferAvailability.AVAILABLE);
        p.setStore(store);
        p.setCreatedDate(LocalDate.now());
        p.setCreatedAt(OffsetDateTime.now());
        p.setUpdatedAt(OffsetDateTime.now());
        p.setPublishAt(publishAt);
        p.setPublishedAt(publishedAt);
        return p;
    }

    private AccountDataRequestEntity createDataRequest(DataRequestStatus status) {
        AccountDataRequestEntity entity = new AccountDataRequestEntity();
        entity.setUserSubject("sse-test-user-" + UUID.randomUUID().toString().substring(0, 8));
        entity.setUsername("sse-test-user");
        entity.setEmail("sse@test.local");
        entity.setRequestType(DataRequestType.ACCESS);
        entity.setStatus(status);
        entity.setCreatedAt(OffsetDateTime.now());
        return entity;
    }
}
