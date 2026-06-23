package br.com.descontovivo.engagement.service;

import br.com.descontovivo.engagement.api.PromotionVoteResponse;
import br.com.descontovivo.engagement.entity.PromotionVoteEntity;
import br.com.descontovivo.engagement.entity.VoteType;
import br.com.descontovivo.engagement.repository.PromotionVoteRepository;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;

@ApplicationScoped
public class PromotionVoteService {

    private final PromotionRepository promotionRepository;
    private final PromotionVoteRepository voteRepository;
    private final CurrentUserProvider currentUserProvider;

    public PromotionVoteService(PromotionRepository promotionRepository,
                                PromotionVoteRepository voteRepository,
                                CurrentUserProvider currentUserProvider) {
        this.promotionRepository = promotionRepository;
        this.voteRepository = voteRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public PromotionVoteResponse vote(String slug, String type) {
        var user = currentUserProvider.requireVerifiedUser();
        var promotion = findPublished(slug);
        var userSubject = user.subject();
        var voteType = VoteType.valueOf(type);
        var now = OffsetDateTime.now();

        var existing = voteRepository.findByPromotionAndClient(promotion.getId(), userSubject);

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
            vote.setClientId(userSubject);
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

        return new PromotionVoteResponse(
                promotion.getLikesCount(), promotion.getDislikesCount(), voteType.name());
    }

    @Transactional
    public PromotionVoteResponse removeVote(String slug) {
        var user = currentUserProvider.requireVerifiedUser();
        var promotion = findPublished(slug);
        var existing = voteRepository.findByPromotionAndClient(promotion.getId(), user.subject());

        if (existing.isPresent()) {
            var vote = existing.get();
            if (vote.getVoteType() == VoteType.LIKE) {
                promotion.setLikesCount(promotion.getLikesCount() - 1);
            } else {
                promotion.setDislikesCount(promotion.getDislikesCount() - 1);
            }
            voteRepository.delete(vote);
        }

        return new PromotionVoteResponse(
                promotion.getLikesCount(), promotion.getDislikesCount(), null);
    }

    private PromotionEntity findPublished(String slug) {
        return promotionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Published promotion not found: " + slug));
    }
}
