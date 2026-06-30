package br.com.descontovivo.promotion.service;

import br.com.descontovivo.promotion.api.admin.backfill.PromotionImageBackfillItem;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.upload.service.RemoteImageImportService;
import br.com.descontovivo.upload.service.RemoteImageImportService.ImportedImage;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.OffsetDateTime;

@ApplicationScoped
public class PromotionImageBackfillItemProcessor {

    private final PromotionRepository promotionRepository;
    private final RemoteImageImportService remoteImageImportService;

    public PromotionImageBackfillItemProcessor(PromotionRepository promotionRepository,
                                               RemoteImageImportService remoteImageImportService) {
        this.promotionRepository = promotionRepository;
        this.remoteImageImportService = remoteImageImportService;
    }

    @Transactional(Transactional.TxType.REQUIRES_NEW)
    public PromotionImageBackfillItem process(PromotionEntity promotion) {
        String oldImageUrl = promotion.getImageUrl();

        ImportedImage imported = remoteImageImportService.importImage(oldImageUrl);

        PromotionEntity managed = promotionRepository.findById(promotion.getId());
        managed.setImageKey(imported.imageKey());
        managed.setImageUrl(imported.imageUrl());
        managed.setUpdatedAt(OffsetDateTime.now());
        promotionRepository.persist(managed);

        return PromotionImageBackfillItem.success(
                managed.getId(), managed.getSlug(), managed.getTitle(),
                oldImageUrl, imported.imageUrl(), imported.imageKey());
    }
}
