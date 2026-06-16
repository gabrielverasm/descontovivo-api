package br.com.descontovivo.moderation.api;

import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import br.com.descontovivo.moderation.repository.ModerationLogRepository;
import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.store.repository.StoreRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.OffsetDateTime;
import java.util.UUID;

@Path("/api/v1/moderation/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class ModerationResource {

    @ConfigProperty(name = "app.admin-token", defaultValue = "dev-admin-token")
    String adminToken;

    private final PromotionRepository promotionRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final StoreRepository storeRepository;

    public ModerationResource(PromotionRepository promotionRepository,
                              ModerationLogRepository moderationLogRepository,
                              StoreRepository storeRepository) {
        this.promotionRepository = promotionRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.storeRepository = storeRepository;
    }

    @GET
    public Response list(@HeaderParam("X-Admin-Token") String token,
                         @QueryParam("status") @DefaultValue("PENDING_REVIEW") String status,
                         @QueryParam("page") @DefaultValue("0") int page,
                         @QueryParam("size") @DefaultValue("20") int size) {
        validateToken(token);
        var items = promotionRepository.listByStatus(PromotionStatus.valueOf(status), page, Math.min(size, 100));
        return Response.ok(items.stream().map(br.com.descontovivo.promotion.api.PromotionDetailResponse::from).toList()).build();
    }

    @PATCH
    @Path("/{id}")
    @Transactional
    public Response moderate(@HeaderParam("X-Admin-Token") String token,
                             @PathParam("id") UUID id,
                             @Valid ModerationActionRequest request) {
        validateToken(token);

        var entity = promotionRepository.findById(id);
        if (entity == null) throw new NotFoundException("Promotion not found: " + id);

        var now = OffsetDateTime.now();

        switch (request.action()) {
            case APPROVE -> {
                entity.setStatus(PromotionStatus.PUBLISHED);
                entity.setPublishedAt(now);
            }
            case REJECT -> {
                entity.setStatus(PromotionStatus.REJECTED);
                entity.setRejectedAt(now);
            }
            case REMOVE -> {
                entity.setStatus(PromotionStatus.REMOVED);
                entity.setRemovedAt(now);
            }
            case EDIT -> applyEdits(entity, request);
        }

        entity.setUpdatedAt(now);

        var log = new ModerationLogEntity();
        log.setTargetType("PROMOTION");
        log.setTargetId(id);
        log.setAction(request.action().name());
        log.setReason(request.reason());
        log.setActor("admin");
        log.setCreatedAt(now);
        moderationLogRepository.persist(log);

        return Response.ok(br.com.descontovivo.promotion.api.PromotionDetailResponse.from(entity)).build();
    }

    private void applyEdits(br.com.descontovivo.promotion.entity.PromotionEntity entity, ModerationActionRequest req) {
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
    }

    private void validateToken(String token) {
        if (token == null || !token.trim().equals(adminToken.trim())) {
            throw new ForbiddenException("Invalid or missing admin token");
        }
    }
}
