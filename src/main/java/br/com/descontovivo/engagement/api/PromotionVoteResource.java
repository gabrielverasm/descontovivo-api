package br.com.descontovivo.engagement.api;

import br.com.descontovivo.engagement.entity.PromotionVoteEntity;
import br.com.descontovivo.engagement.entity.VoteType;
import br.com.descontovivo.engagement.repository.PromotionVoteRepository;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.time.OffsetDateTime;

@Path("/api/v1/promotions/{slug}/vote")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class PromotionVoteResource {

    private final PromotionRepository promotionRepository;
    private final PromotionVoteRepository voteRepository;

    public PromotionVoteResource(PromotionRepository promotionRepository,
                                 PromotionVoteRepository voteRepository) {
        this.promotionRepository = promotionRepository;
        this.voteRepository = voteRepository;
    }

    @PUT
    @Transactional
    public Response vote(@PathParam("slug") String slug, @Valid PromotionVoteRequest request) {
        var promotion = findPublished(slug);
        var voteType = VoteType.valueOf(request.type());
        var now = OffsetDateTime.now();

        var existing = voteRepository.findByPromotionAndClient(promotion.getId(), request.clientId());

        if (existing.isPresent()) {
            var vote = existing.get();
            if (vote.getVoteType() != voteType) {
                if (vote.getVoteType() == VoteType.LIKE) {
                    promotion.setLikesCount(promotion.getLikesCount() - 1);
                } else {
                    promotion.setDislikesCount(promotion.getDislikesCount() - 1);
                }
                vote.setVoteType(voteType);
                vote.setUpdatedAt(now);
                if (voteType == VoteType.LIKE) {
                    promotion.setLikesCount(promotion.getLikesCount() + 1);
                } else {
                    promotion.setDislikesCount(promotion.getDislikesCount() + 1);
                }
            }
        } else {
            var vote = new PromotionVoteEntity();
            vote.setPromotion(promotion);
            vote.setClientId(request.clientId());
            vote.setVoteType(voteType);
            vote.setCreatedAt(now);
            vote.setUpdatedAt(now);
            voteRepository.persist(vote);
            if (voteType == VoteType.LIKE) {
                promotion.setLikesCount(promotion.getLikesCount() + 1);
            } else {
                promotion.setDislikesCount(promotion.getDislikesCount() + 1);
            }
        }

        return Response.ok(new PromotionVoteResponse(
                promotion.getLikesCount(), promotion.getDislikesCount(), voteType.name()
        )).build();
    }

    @DELETE
    @Transactional
    public Response removeVote(@PathParam("slug") String slug, @QueryParam("clientId") String clientId) {
        var promotion = findPublished(slug);
        var existing = voteRepository.findByPromotionAndClient(promotion.getId(), clientId);

        if (existing.isPresent()) {
            var vote = existing.get();
            if (vote.getVoteType() == VoteType.LIKE) {
                promotion.setLikesCount(promotion.getLikesCount() - 1);
            } else {
                promotion.setDislikesCount(promotion.getDislikesCount() - 1);
            }
            voteRepository.delete(vote);
        }

        return Response.ok(new PromotionVoteResponse(
                promotion.getLikesCount(), promotion.getDislikesCount(), null
        )).build();
    }

    private PromotionEntity findPublished(String slug) {
        return promotionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Published promotion not found: " + slug));
    }
}
