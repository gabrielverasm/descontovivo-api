package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.PromotionNormalizer;
import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.shared.api.ConflictException;
import br.com.descontovivo.shared.api.PagedResponse;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import br.com.descontovivo.store.repository.StoreRepository;
import io.quarkus.security.Authenticated;
import jakarta.annotation.security.PermitAll;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;

@Path("/api/v1/promotions")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionResource {

    private static final ZoneId SAO_PAULO = ZoneId.of("America/Sao_Paulo");

    private final PromotionRepository promotionRepository;
    private final StoreRepository storeRepository;
    private final CurrentUserProvider currentUserProvider;

    public PromotionResource(PromotionRepository promotionRepository,
                             StoreRepository storeRepository,
                             CurrentUserProvider currentUserProvider) {
        this.promotionRepository = promotionRepository;
        this.storeRepository = storeRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @GET
    @PermitAll
    public PagedResponse<PromotionSummaryResponse> list(
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size,
            @QueryParam("store") String store,
            @QueryParam("availability") String availability,
            @QueryParam("q") String q) {

        size = Math.min(size, 100);
        var items = promotionRepository.listPublished(page, size, store, availability, q)
                .stream().map(PromotionSummaryResponse::from).toList();
        long total = promotionRepository.countPublished(store, availability, q);
        return PagedResponse.of(items, page, size, total);
    }

    @GET
    @Path("/{slug}")
    @PermitAll
    public PromotionDetailResponse getBySlug(@PathParam("slug") String slug) {
        return promotionRepository.findPublishedBySlug(slug)
                .map(PromotionDetailResponse::from)
                .orElseThrow(() -> new NotFoundException("Promotion not found: " + slug));
    }

    @POST
    @Transactional
    @Authenticated
    public Response create(@Valid PromotionCreateRequest request) {
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

        return Response.status(201).entity(PromotionDetailResponse.from(entity)).build();
    }
}
