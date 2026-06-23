package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.api.PromotionCreateRequest;
import br.com.descontovivo.promotion.api.PromotionDetailResponse;
import br.com.descontovivo.promotion.api.PromotionSummaryResponse;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.shared.api.ConflictException;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import br.com.descontovivo.store.repository.StoreRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class PromotionService {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final PromotionRepository promotionRepository;
    private final StoreRepository storeRepository;
    private final CurrentUserProvider currentUserProvider;

    public PromotionService(PromotionRepository promotionRepository,
                            StoreRepository storeRepository,
                            CurrentUserProvider currentUserProvider) {
        this.promotionRepository = promotionRepository;
        this.storeRepository = storeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public List<PromotionSummaryResponse> listPublished(int page, int size, String store, String availability, String q) {
        return promotionRepository.listPublished(page, size, store, availability, q)
                .stream().map(PromotionSummaryResponse::from).toList();
    }

    public long countPublished(String store, String availability, String q) {
        return promotionRepository.countPublished(store, availability, q);
    }

    @Transactional
    public PromotionDetailResponse findPublishedBySlug(String slug) {
        var entity = promotionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + slug));
        return PromotionDetailResponse.from(entity);
    }

    @Transactional
    public PromotionDetailResponse create(PromotionCreateRequest request) {
        var user = currentUserProvider.requireVerifiedUser();

        var store = storeRepository.findBySlug(request.storeSlug())
                .orElseThrow(() -> new NotFoundException("Store not found: " + request.storeSlug()));

        String normalizedUrl = PromotionNormalizer.normalizeUrl(request.url());
        String normalizedDescription = PromotionNormalizer.normalizeDescription(request.description());
        LocalDate today = LocalDate.now(SAO_PAULO);

        if (promotionRepository.existsDuplicate(normalizedUrl, normalizedDescription, today)) {
            throw new ConflictException("Duplicate promotion: same URL and description already posted today");
        }

        String slug = SlugGenerator.fromTitle(request.title());
        if (promotionRepository.count("slug", slug) > 0) {
            slug = SlugGenerator.withSuffix(slug);
        }

        var now = OffsetDateTime.now();
        var entity = new PromotionEntity();
        entity.setSlug(slug);
        entity.setTitle(request.title());
        entity.setUrl(request.url());
        entity.setNormalizedUrl(normalizedUrl);
        entity.setDescription(request.description());
        entity.setNormalizedDescription(normalizedDescription);
        entity.setCurrentPrice(request.currentPrice());
        entity.setOriginalPrice(request.originalPrice());
        entity.setCouponCode(request.couponCode());
        entity.setImageUrl(request.imageUrl());
        entity.setStatus(PromotionStatus.PENDING_REVIEW);
        entity.setAvailability(OfferAvailability.AVAILABLE);
        entity.setStore(store);
        entity.setCreatedDate(today);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);

        promotionRepository.persist(entity);
        return PromotionDetailResponse.from(entity);
    }
}
