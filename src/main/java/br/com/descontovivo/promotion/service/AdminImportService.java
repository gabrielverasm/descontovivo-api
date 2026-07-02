package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.api.admin.AdminImportError;
import br.com.descontovivo.promotion.api.admin.AdminImportItemRequest;
import br.com.descontovivo.promotion.api.admin.AdminImportRequest;
import br.com.descontovivo.promotion.api.admin.AdminImportResponse;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.store.service.StoreResolver;
import br.com.descontovivo.upload.service.RemoteImageImportService;
import br.com.descontovivo.upload.service.RemoteImageImportService.ImportedImage;
import br.com.descontovivo.upload.service.RemoteImageImportService.RemoteImageException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.*;

@ApplicationScoped
public class AdminImportService {

    private static final Logger LOG = Logger.getLogger(AdminImportService.class);
    private static final String SOURCE = "ADMIN_JSON_IMPORT";
    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final PromotionRepository promotionRepository;
    private final StoreResolver storeResolver;
    private final RemoteImageImportService remoteImageImportService;

    @ConfigProperty(name = "admin.import.default-author", defaultValue = "gabrielveras")
    String defaultAuthor;

    public AdminImportService(PromotionRepository promotionRepository,
                              StoreResolver storeResolver,
                              RemoteImageImportService remoteImageImportService) {
        this.promotionRepository = promotionRepository;
        this.storeResolver = storeResolver;
        this.remoteImageImportService = remoteImageImportService;
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

        // Intra-batch dedup sets
        var seenSourceIds = new HashSet<String>();
        var seenNormalizedUrls = new HashSet<String>();

        for (var item : request.items()) {
            var itemErrors = validate(item);
            if (!itemErrors.isEmpty()) {
                errors.addAll(itemErrors);
                continue;
            }

            // Intra-batch sourceId dedup
            if (!seenSourceIds.add(item.sourceId())) {
                skipped++;
                continue;
            }

            // DB sourceId dedup
            if (promotionRepository.existsBySourceId(item.sourceId())) {
                skipped++;
                continue;
            }

            String normalizedUrl = PromotionNormalizer.normalizeUrl(item.productUrl());

            // Intra-batch URL dedup
            if (!seenNormalizedUrls.add(normalizedUrl)) {
                skipped++;
                continue;
            }

            // DB URL dedup
            if (promotionRepository.existsByNormalizedUrl(normalizedUrl)) {
                skipped++;
                continue;
            }

            if (!dryRun) {
                // Import image from external URL to R2
                ImportedImage importedImage;
                try {
                    importedImage = remoteImageImportService.importImage(item.imageUrl());
                } catch (RemoteImageException e) {
                    errors.add(new AdminImportError(item.sourceId(), "imageUrl", e.getMessage()));
                    continue;
                }
                persist(item, batchId, importStartedAt, normalizedUrl, importedImage, callerUsername);
            } else {
                // Dry run: validate URL format and host only
                try {
                    remoteImageImportService.validateUrlForDryRun(item.imageUrl());
                } catch (RemoteImageException e) {
                    errors.add(new AdminImportError(item.sourceId(), "imageUrl", e.getMessage()));
                    continue;
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
        entity.setTitle(item.title());
        entity.setUrl(item.productUrl());
        entity.setNormalizedUrl(normalizedUrl);
        entity.setCurrentPrice(item.currentPrice());
        entity.setOriginalPrice(item.originalPrice());
        entity.setCouponCode(item.coupon());
        entity.setImageUrl(importedImage.imageUrl());
        entity.setImageKey(importedImage.imageKey());
        entity.setStatus(PromotionStatus.PUBLISHED);
        entity.setAvailability(OfferAvailability.AVAILABLE);
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

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
