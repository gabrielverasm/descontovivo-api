package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.api.admin.AdminImportError;
import br.com.descontovivo.promotion.api.admin.AdminImportItemRequest;
import br.com.descontovivo.promotion.api.admin.AdminImportRequest;
import br.com.descontovivo.promotion.api.admin.AdminImportResponse;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionPriceSignal;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.promotion.support.TrustSignalsHelper;
import br.com.descontovivo.store.service.StoreResolver;
import br.com.descontovivo.upload.service.R2StorageService;
import br.com.descontovivo.upload.service.RemoteImageImportService;
import br.com.descontovivo.upload.service.RemoteImageImportService.ImportedImage;
import br.com.descontovivo.upload.service.RemoteImageImportService.RemoteImageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.regex.Pattern;

@ApplicationScoped
public class AdminImportService {

    private static final Logger LOG = Logger.getLogger(AdminImportService.class);
    private static final String SOURCE = "ADMIN_JSON_IMPORT";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");
    private static final Duration DUPLICATE_BLOCK_WINDOW = Duration.ofHours(24);

    /** Only accept imageKey with safe characters and expected prefix. */
    private static final Pattern SAFE_IMAGE_KEY = Pattern.compile(
            "^temp/promotions/\\d{4}/\\d{2}/[a-f0-9\\-]+\\.webp$"
    );

    private final PromotionRepository promotionRepository;
    private final StoreResolver storeResolver;
    private final RemoteImageImportService remoteImageImportService;
    private final R2StorageService r2StorageService;

    @ConfigProperty(name = "admin.import.default-author", defaultValue = "gabrielveras")
    String defaultAuthor;

    public AdminImportService(PromotionRepository promotionRepository,
                              StoreResolver storeResolver,
                              RemoteImageImportService remoteImageImportService,
                              R2StorageService r2StorageService) {
        this.promotionRepository = promotionRepository;
        this.storeResolver = storeResolver;
        this.remoteImageImportService = remoteImageImportService;
        this.r2StorageService = r2StorageService;
    }

    @Transactional
    public AdminImportResponse executePersistent(AdminImportRequest request, String callerUsername) {
        return execute(request, false, callerUsername);
    }

    public AdminImportResponse executeDryRun(AdminImportRequest request, String callerUsername) {
        return execute(request, true, callerUsername);
    }

    private AdminImportResponse execute(AdminImportRequest request, boolean dryRun, String callerUsername) {
        String batchId = request.batchId() != null && !request.batchId().isBlank()
                ? request.batchId()
                : "import-" + UUID.randomUUID().toString().substring(0, 12);

        var importStartedAt = OffsetDateTime.now();
        var errors = new ArrayList<AdminImportError>();
        int created = 0;
        int skipped = 0;

        // Publication timestamps already accepted in this batch, grouped by dedup key.
        var seenSourceIds = new HashMap<String, List<OffsetDateTime>>();
        var seenNormalizedUrls = new HashMap<String, List<OffsetDateTime>>();

        for (var item : request.items()) {
            var itemErrors = validate(item);
            if (!itemErrors.isEmpty()) {
                errors.addAll(itemErrors);
                continue;
            }

            String normalizedUrl = PromotionNormalizer.normalizeUrl(item.productUrl());
            OffsetDateTime publishedAt = item.publishAt() != null ? item.publishAt() : importStartedAt;

            warnAboutFuturePublications(seenSourceIds.get(item.sourceId()), item.sourceId(), normalizedUrl, importStartedAt);
            warnAboutFuturePublications(seenNormalizedUrls.get(normalizedUrl), item.sourceId(), normalizedUrl, importStartedAt);
            if (hasRecentPublication(seenSourceIds.get(item.sourceId()), importStartedAt)
                    || hasRecentPublication(seenNormalizedUrls.get(normalizedUrl), importStartedAt)) {
                skipped++;
                continue;
            }

            var equivalentPublishedAt = promotionRepository.findRelevantEquivalentPublishedAt(
                    item.sourceId(), normalizedUrl, importStartedAt.minus(DUPLICATE_BLOCK_WINDOW));
            warnAboutFuturePublications(equivalentPublishedAt, item.sourceId(), normalizedUrl, importStartedAt);
            if (hasRecentPublication(equivalentPublishedAt, importStartedAt)) {
                skipped++;
                continue;
            }

            seenSourceIds.computeIfAbsent(item.sourceId(), ignored -> new ArrayList<>()).add(publishedAt);
            seenNormalizedUrls.computeIfAbsent(normalizedUrl, ignored -> new ArrayList<>()).add(publishedAt);

            if (!dryRun) {
                // If imageKey is provided (already uploaded by UI), promote from temp and skip remote import
                ImportedImage importedImage;
                if (hasValidImageKey(item.imageKey())) {
                    try {
                        String finalKey = r2StorageService.promoteImage(item.imageKey());
                        String publicUrl = r2StorageService.buildPublicUrl(finalKey);
                        importedImage = new ImportedImage(finalKey, publicUrl, "image/webp", 0);
                        LOG.infof("Image bypassed remote import (pre-uploaded): %s -> %s", item.imageKey(), finalKey);
                    } catch (Exception e) {
                        errors.add(new AdminImportError(item.sourceId(), "imageKey",
                                "Falha ao promover imagem pré-enviada: " + e.getMessage()));
                        continue;
                    }
                } else {
                    // Import image from external URL to R2
                    try {
                        importedImage = remoteImageImportService.importImage(item.imageUrl());
                    } catch (RemoteImageException e) {
                        errors.add(new AdminImportError(item.sourceId(), "imageUrl", e.getMessage()));
                        continue;
                    }
                }
                persist(item, batchId, importStartedAt, normalizedUrl, importedImage, callerUsername);
            } else {
                // Dry run: skip validation if imageKey is present (already uploaded)
                if (!hasValidImageKey(item.imageKey())) {
                    try {
                        remoteImageImportService.validateUrlForDryRun(item.imageUrl());
                    } catch (RemoteImageException e) {
                        errors.add(new AdminImportError(item.sourceId(), "imageUrl", e.getMessage()));
                        continue;
                    }
                }
            }
            created++;
        }

        return new AdminImportResponse(batchId, dryRun, created, skipped, errors);
    }

    private void persist(AdminImportItemRequest item, String batchId, OffsetDateTime importStartedAt, String normalizedUrl, ImportedImage importedImage, String callerUsername) {
        var store = storeResolver.findOrCreateByName(item.storeName());

        String slug = generateUniqueSlug(item.title());
        var now = OffsetDateTime.now();
        OffsetDateTime publishAt = item.publishAt() != null ? item.publishAt() : importStartedAt;
        OffsetDateTime verifiedAt = item.verifiedAt() != null ? item.verifiedAt() : importStartedAt;

        var entity = new PromotionEntity();
        entity.setSlug(slug);
        entity.setTitle(PromotionNormalizer.normalizeTitle(item.title()));
        entity.setUrl(item.productUrl());
        entity.setNormalizedUrl(normalizedUrl);
        entity.setCurrentPrice(item.currentPrice());
        entity.setOriginalPrice(item.originalPrice());
        entity.setCouponCode(item.coupon());
        entity.setImageUrl(importedImage.imageUrl());
        entity.setImageKey(importedImage.imageKey());
        entity.setStatus(PromotionStatus.PUBLISHED);
        entity.setAvailability(resolveAvailability(item.availability()));
        entity.setPriceSignal(resolvePriceSignal(item.priceSignal()));
        entity.setStore(store);
        entity.setCreatedDate(LocalDate.now(SAO_PAULO));
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setPublishedAt(publishAt);
        entity.setPublishAt(publishAt);
        entity.setVerifiedAt(verifiedAt);
        entity.setSource(SOURCE);
        entity.setSourceId(item.sourceId());
        entity.setBatchId(batchId);
        entity.setAuthorUsername(callerUsername != null && !callerUsername.isBlank() ? callerUsername : defaultAuthor);
        entity.setMarketplace(item.marketplace());
        entity.setSellerName(item.sellerName());
        entity.setSoldBy(item.soldBy());
        entity.setDeliveredBy(item.deliveredBy());
        entity.setCategory(item.category());
        
        // New trust signals fields
        entity.setSalesCount(item.salesCount());
        entity.setProductRating(item.productRating());
        entity.setSellerRating(item.sellerRating());
        entity.setOfficialStore(item.officialStore() != null ? item.officialStore() : false);
        entity.setTrustSignals(item.trustSignals() != null ? 
                TrustSignalsHelper.convertTrustSignalsToJson(
                        TrustSignalsHelper.validateTrustSignals(item.trustSignals(), item.marketplace())
                ) : null);

        promotionRepository.persist(entity);
    }

    private String generateUniqueSlug(String title) {
        String base = SlugGenerator.fromTitle(title);
        if (promotionRepository.count("slug", base) == 0) return base;

        for (int i = 2; i <= 100; i++) {
            String candidate = base + "-" + i;
            if (promotionRepository.count("slug", candidate) == 0) return candidate;
        }
        return base + "-" + UUID.randomUUID().toString().substring(0, 6);
    }

    private List<AdminImportError> validate(AdminImportItemRequest item) {
        var errors = new ArrayList<AdminImportError>();
        if (isBlank(item.sourceId())) errors.add(new AdminImportError(item.sourceId(), "sourceId", "sourceId obrigatório"));
        if (isBlank(item.title())) errors.add(new AdminImportError(item.sourceId(), "title", "title obrigatório"));
        if (isBlank(item.productUrl())) errors.add(new AdminImportError(item.sourceId(), "productUrl", "productUrl obrigatório"));
        if (isBlank(item.imageUrl())) errors.add(new AdminImportError(item.sourceId(), "imageUrl", "imageUrl obrigatório"));
        if (isBlank(item.storeName())) errors.add(new AdminImportError(item.sourceId(), "storeName", "storeName obrigatório"));
        if (isBlank(item.marketplace())) errors.add(new AdminImportError(item.sourceId(), "marketplace", "marketplace obrigatório"));
        if (item.currentPrice() == null || item.currentPrice().compareTo(BigDecimal.ZERO) <= 0) {
            errors.add(new AdminImportError(item.sourceId(), "currentPrice", "currentPrice obrigatório e maior que zero"));
        }
        if (item.originalPrice() != null && item.currentPrice() != null
                && item.originalPrice().compareTo(item.currentPrice()) < 0) {
            errors.add(new AdminImportError(item.sourceId(), "originalPrice", "originalPrice não pode ser menor que currentPrice"));
        }
        return errors;
    }

    private static OfferAvailability resolveAvailability(String value) {
        if (value == null || value.isBlank()) return OfferAvailability.AVAILABLE;
        try {
            return OfferAvailability.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return OfferAvailability.AVAILABLE;
        }
    }

    private static PromotionPriceSignal resolvePriceSignal(String value) {
        if (value == null || value.isBlank()) return PromotionPriceSignal.NONE;
        try {
            return PromotionPriceSignal.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return PromotionPriceSignal.NONE;
        }
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }

    private static boolean hasRecentPublication(List<OffsetDateTime> publishedDates, OffsetDateTime now) {
        return publishedDates != null && publishedDates.stream()
                .anyMatch(publishedAt -> isWithinDuplicateWindow(publishedAt, now));
    }

    static boolean isWithinDuplicateWindow(OffsetDateTime publishedAt, OffsetDateTime now) {
        if (publishedAt == null || publishedAt.isAfter(now)) {
            return false;
        }
        return publishedAt.isAfter(now.minus(DUPLICATE_BLOCK_WINDOW));
    }

    private static void warnAboutFuturePublications(List<OffsetDateTime> publishedDates,
                                                    String sourceId,
                                                    String normalizedUrl,
                                                    OffsetDateTime now) {
        if (publishedDates != null && publishedDates.stream()
                .filter(Objects::nonNull)
                .anyMatch(publishedAt -> publishedAt.isAfter(now))) {
            LOG.warnf("Future publishedAt found during admin import dedup; sourceId=%s normalizedUrl=%s",
                    sourceId, normalizedUrl);
        }
    }

    /**
     * Validates that imageKey is non-blank and matches the expected format for
     * pre-uploaded temp images (temp/promotions/YYYY/MM/uuid.webp).
     * Rejects path traversal, arbitrary paths, and unexpected formats.
     */
    private static boolean hasValidImageKey(String imageKey) {
        if (imageKey == null || imageKey.isBlank()) return false;
        return SAFE_IMAGE_KEY.matcher(imageKey).matches();
    }


}
