package br.com.descontovivo.moderation.service;

import br.com.descontovivo.moderation.api.ModerationActionRequest;
import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import br.com.descontovivo.moderation.repository.ModerationLogRepository;
import br.com.descontovivo.promotion.api.PromotionDetailResponse;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import br.com.descontovivo.store.repository.StoreRepository;
import br.com.descontovivo.upload.service.R2StorageService;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PromotionModerationService {

    private final PromotionRepository promotionRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final StoreRepository storeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final R2StorageService r2StorageService;
    private final SecurityIdentity securityIdentity;

    public PromotionModerationService(PromotionRepository promotionRepository,
                                      ModerationLogRepository moderationLogRepository,
                                      StoreRepository storeRepository,
                                      CurrentUserProvider currentUserProvider,
                                      R2StorageService r2StorageService,
                                      SecurityIdentity securityIdentity) {
        this.promotionRepository = promotionRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.storeRepository = storeRepository;
        this.currentUserProvider = currentUserProvider;
        this.r2StorageService = r2StorageService;
        this.securityIdentity = securityIdentity;
    }

    @Transactional
    public List<PromotionDetailResponse> listByStatus(String status, int page, int size) {
        return promotionRepository.listByStatus(PromotionStatus.valueOf(status), page, Math.min(size, 100))
                .stream().map(PromotionDetailResponse::from).toList();
    }

    @Transactional
    public PromotionDetailResponse moderate(UUID id, ModerationActionRequest request) {
        if (request.action() == ModerationActionRequest.ModerationAction.REMOVE
                && !securityIdentity.hasRole("admin")) {
            throw new ForbiddenException("Only admin can remove promotions");
        }

        var user = currentUserProvider.currentUser();

        var entity = promotionRepository.findById(id);
        if (entity == null) throw new NotFoundException("Promotion not found: " + id);

        var now = OffsetDateTime.now();

        switch (request.action()) {
            case APPROVE -> {
                entity.setStatus(PromotionStatus.PUBLISHED);
                entity.setPublishedAt(now);
                if (entity.getPublishAt() == null || entity.getPublishAt().isAfter(now)) {
                    entity.setPublishAt(now);
                }
            }
            case REJECT -> {
                entity.setStatus(PromotionStatus.REJECTED);
                entity.setRejectedAt(now);
                r2StorageService.deletePromotionImageIfPresent(entity.getImageKey());
            }
            case REMOVE -> {
                entity.setStatus(PromotionStatus.REMOVED);
                entity.setRemovedAt(now);
                r2StorageService.deletePromotionImageIfPresent(entity.getImageKey());
            }
            case EDIT -> applyEdits(entity, request);
        }

        entity.setUpdatedAt(now);

        var log = new ModerationLogEntity();
        log.setTargetType("PROMOTION");
        log.setTargetId(id);
        log.setAction(request.action().name());
        log.setReason(request.reason());
        log.setActor(user.username() != null ? user.username() : user.subject());
        log.setCreatedAt(now);
        moderationLogRepository.persist(log);

        return PromotionDetailResponse.from(entity);
    }

    private void applyEdits(PromotionEntity entity, ModerationActionRequest req) {
        if (req.title() != null) entity.setTitle(req.title());
        if (req.url() != null) {
            entity.setUrl(req.url());
            entity.setNormalizedUrl(PromotionNormalizer.normalizeUrl(req.url()));
        }
        if (req.description() != null) {
            entity.setDescription(req.description());
            entity.setNormalizedDescription(PromotionNormalizer.normalizeDescription(req.description()));
        }
        if (req.currentPrice() != null) entity.setCurrentPrice(req.currentPrice());
        if (req.originalPrice() != null) entity.setOriginalPrice(req.originalPrice());
        if (req.couponCode() != null) entity.setCouponCode(req.couponCode());
        if (req.imageUrl() != null) entity.setImageUrl(req.imageUrl());
        if (req.availability() != null) entity.setAvailability(OfferAvailability.valueOf(req.availability()));
        if (req.storeSlug() != null) {
            var store = storeRepository.findBySlug(req.storeSlug())
                    .orElseThrow(() -> new NotFoundException("Store not found: " + req.storeSlug()));
            entity.setStore(store);
        }
        if (req.soldBy() != null) entity.setSoldBy(req.soldBy());
        if (req.deliveredBy() != null) entity.setDeliveredBy(req.deliveredBy());
        if (req.category() != null) entity.setCategory(req.category());
    }
}
