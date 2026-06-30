package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.api.admin.backfill.PromotionImageBackfillItem;
import br.com.descontovivo.promotion.api.admin.backfill.PromotionImageBackfillResponse;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.upload.config.R2Config;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class PromotionImageBackfillService {

    private static final Logger LOG = Logger.getLogger(PromotionImageBackfillService.class);
    private static final int MAX_LIMIT = 100;

    private final PromotionRepository promotionRepository;
    private final PromotionImageBackfillItemProcessor itemProcessor;
    private final R2Config r2Config;

    public PromotionImageBackfillService(PromotionRepository promotionRepository,
                                         PromotionImageBackfillItemProcessor itemProcessor,
                                         R2Config r2Config) {
        this.promotionRepository = promotionRepository;
        this.itemProcessor = itemProcessor;
        this.r2Config = r2Config;
    }

    public PromotionImageBackfillResponse execute(boolean dryRun, int limit) {
        int effectiveLimit = Math.min(limit, MAX_LIMIT);

        LOG.infof("Image backfill started: dryRun=%s, limit=%d (effective=%d)", dryRun, limit, effectiveLimit);

        List<PromotionEntity> eligible = findEligible(effectiveLimit);

        LOG.infof("Image backfill: found %d eligible promotions with external images", eligible.size());

        if (dryRun) {
            return executeDryRun(eligible);
        }

        return executeReal(eligible);
    }

    public int getMaxLimit() {
        return MAX_LIMIT;
    }

    private List<PromotionEntity> findEligible(int limit) {
        String r2BaseUrl = r2Config.publicBaseUrl();
        return promotionRepository.findWithExternalImage(r2BaseUrl, limit);
    }

    private PromotionImageBackfillResponse executeDryRun(List<PromotionEntity> eligible) {
        var items = eligible.stream()
                .map(p -> PromotionImageBackfillItem.eligible(p.getId(), p.getSlug(), p.getTitle(), p.getImageUrl()))
                .toList();

        LOG.infof("Image backfill dry run complete: %d eligible promotions", eligible.size());

        return new PromotionImageBackfillResponse(true, eligible.size(), eligible.size(), 0, 0, items);
    }

    private PromotionImageBackfillResponse executeReal(List<PromotionEntity> eligible) {
        var items = new ArrayList<PromotionImageBackfillItem>();
        int updated = 0;
        int failed = 0;

        for (PromotionEntity promotion : eligible) {
            try {
                var result = itemProcessor.process(promotion);
                items.add(result);
                updated++;
                LOG.infof("Image backfill success: slug=%s, id=%s, newKey=%s",
                        promotion.getSlug(), promotion.getId(), result.imageKey());
            } catch (Exception e) {
                failed++;
                items.add(PromotionImageBackfillItem.failed(
                        promotion.getId(), promotion.getSlug(), promotion.getTitle(),
                        promotion.getImageUrl(), e.getMessage()));
                LOG.warnf("Image backfill failed: slug=%s, id=%s, error=%s",
                        promotion.getSlug(), promotion.getId(), e.getMessage());
            }
        }

        LOG.infof("Image backfill complete: eligible=%d, updated=%d, failed=%d",
                eligible.size(), updated, failed);

        return new PromotionImageBackfillResponse(false, eligible.size(), eligible.size(), updated, failed, items);
    }
}
