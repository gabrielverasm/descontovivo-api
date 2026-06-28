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
import br.com.descontovivo.store.entity.StoreEntity;
import br.com.descontovivo.store.repository.StoreRepository;
import br.com.descontovivo.upload.service.R2StorageService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.net.URI;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class PromotionService {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final String FALLBACK_STORE_SLUG = "loja-nao-identificada";

    private static final Map<String, String> DOMAIN_TO_STORE_SLUG = Map.of(
            "amazon.com.br", "amazon",
            "mercadolivre.com.br", "mercado-livre",
            "magazineluiza.com.br", "magalu"
    );

    private final PromotionRepository promotionRepository;
    private final StoreRepository storeRepository;
    private final CurrentUserProvider currentUserProvider;
    private final R2StorageService r2StorageService;

    public PromotionService(PromotionRepository promotionRepository,
                            StoreRepository storeRepository,
                            CurrentUserProvider currentUserProvider,
                            R2StorageService r2StorageService) {
        this.promotionRepository = promotionRepository;
        this.storeRepository = storeRepository;
        this.currentUserProvider = currentUserProvider;
        this.r2StorageService = r2StorageService;
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

        StoreEntity store = resolveStore(request.storeSlug(), request.url());

        String description = (request.description() != null && !request.description().isBlank())
                ? request.description() : request.title();

        String normalizedUrl = PromotionNormalizer.normalizeUrl(request.url());
        LocalDate today = LocalDate.now(SAO_PAULO);

        if (promotionRepository.existsDuplicateByUrl(normalizedUrl, today)) {
            throw new ConflictException("Duplicate promotion: same URL already posted today");
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
        entity.setDescription(description);
        entity.setNormalizedDescription(PromotionNormalizer.normalizeDescription(description));
        entity.setCurrentPrice(request.currentPrice());
        entity.setOriginalPrice(request.originalPrice());
        entity.setCouponCode(request.couponCode());
        String finalImageKey = r2StorageService.promoteImage(request.imageKey());
        entity.setImageKey(finalImageKey);
        entity.setImageUrl(r2StorageService.buildPublicUrl(finalImageKey));
        entity.setStatus(PromotionStatus.PENDING_REVIEW);
        entity.setAvailability(OfferAvailability.AVAILABLE);
        entity.setStore(store);
        entity.setCreatedDate(today);
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setPublishAt(now);

        promotionRepository.persist(entity);
        return PromotionDetailResponse.from(entity);
    }

    private StoreEntity resolveStore(String storeSlug, String url) {
        if (storeSlug != null && !storeSlug.isBlank()) {
            return storeRepository.findBySlug(storeSlug)
                    .orElseThrow(() -> new NotFoundException("Store not found: " + storeSlug));
        }

        Optional<StoreEntity> inferred = inferStoreFromUrl(url);
        if (inferred.isPresent()) {
            return inferred.get();
        }

        return storeRepository.findBySlug(FALLBACK_STORE_SLUG)
                .orElseThrow(() -> new IllegalStateException("Fallback store not found"));
    }

    private Optional<StoreEntity> inferStoreFromUrl(String url) {
        try {
            String host = URI.create(url).getHost();
            if (host == null) return Optional.empty();
            host = host.toLowerCase().replaceFirst("^www\\.", "");
            String slug = DOMAIN_TO_STORE_SLUG.get(host);
            if (slug != null) {
                return storeRepository.findBySlug(slug);
            }
        } catch (Exception ignored) {
        }
        return Optional.empty();
    }
}
