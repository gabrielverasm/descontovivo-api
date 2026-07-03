package br.com.descontovivo.notification.service;

import br.com.descontovivo.account.repository.AccountDataRequestRepository;
import br.com.descontovivo.notification.dto.AdminDataRequestSnapshot;
import br.com.descontovivo.notification.dto.ModerationPromotionSnapshot;
import br.com.descontovivo.notification.dto.PublicPromotionSnapshot;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

/**
 * Builds lightweight snapshots for SSE notification streams.
 * All queries use count/aggregate operations — no list loading.
 */
@ApplicationScoped
public class NotificationSnapshotService {

    private final PromotionRepository promotionRepository;
    private final AccountDataRequestRepository dataRequestRepository;

    public NotificationSnapshotService(PromotionRepository promotionRepository,
                                       AccountDataRequestRepository dataRequestRepository) {
        this.promotionRepository = promotionRepository;
        this.dataRequestRepository = dataRequestRepository;
    }

    /**
     * Public snapshot: published promotions count + latest publishedAt.
     * Safe for unauthenticated consumers — contains only aggregate public data.
     */
    @Transactional
    public PublicPromotionSnapshot publicPromotionSnapshot() {
        long count = promotionRepository.countPublishedVisible();
        var latestPublishedAt = promotionRepository.findLatestPublishedAt().orElse(null);
        return new PublicPromotionSnapshot(count, latestPublishedAt);
    }

    /**
     * Moderation snapshot: count of promotions pending review.
     * Intended for moderator/admin roles only.
     */
    @Transactional
    public ModerationPromotionSnapshot moderationPromotionSnapshot() {
        long pending = promotionRepository.countPendingModeration();
        return new ModerationPromotionSnapshot(pending);
    }

    /**
     * Admin data-request snapshot: count of PENDING + IN_REVIEW requests.
     * Intended for admin role only.
     */
    @Transactional
    public AdminDataRequestSnapshot adminDataRequestSnapshot() {
        long open = dataRequestRepository.countOpenRequests();
        return new AdminDataRequestSnapshot(open);
    }
}
