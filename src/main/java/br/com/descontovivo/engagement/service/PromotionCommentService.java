package br.com.descontovivo.engagement.service;

import br.com.descontovivo.engagement.api.PromotionCommentCreateRequest;
import br.com.descontovivo.engagement.api.PromotionCommentResponse;
import br.com.descontovivo.engagement.entity.PromotionCommentEntity;
import br.com.descontovivo.engagement.repository.PromotionCommentRepository;
import br.com.descontovivo.moderation.entity.ModerationLogEntity;
import br.com.descontovivo.moderation.repository.ModerationLogRepository;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class PromotionCommentService {

    private final PromotionRepository promotionRepository;
    private final PromotionCommentRepository commentRepository;
    private final ModerationLogRepository moderationLogRepository;
    private final CurrentUserProvider currentUserProvider;

    public PromotionCommentService(PromotionRepository promotionRepository,
                                   PromotionCommentRepository commentRepository,
                                   ModerationLogRepository moderationLogRepository,
                                   CurrentUserProvider currentUserProvider) {
        this.promotionRepository = promotionRepository;
        this.commentRepository = commentRepository;
        this.moderationLogRepository = moderationLogRepository;
        this.currentUserProvider = currentUserProvider;
    }

    @Transactional
    public List<PromotionCommentResponse> listByPromotion(String slug) {
        var promotion = findPublished(slug);
        return commentRepository.listByPromotion(promotion.getId())
                .stream().map(PromotionCommentResponse::from).toList();
    }

    @Transactional
    public PromotionCommentResponse createComment(String slug, PromotionCommentCreateRequest request) {
        var user = currentUserProvider.requireVerifiedUser();
        var promotion = findPublished(slug);
        var now = OffsetDateTime.now();

        var comment = new PromotionCommentEntity();
        comment.setPromotion(promotion);
        comment.setClientId(user.subject());
        comment.setAuthorName(request.authorName());
        comment.setContent(request.content());
        comment.setRemoved(false);
        comment.setCreatedAt(now);
        comment.setUpdatedAt(now);
        commentRepository.persist(comment);

        promotion.setCommentsCount(promotion.getCommentsCount() + 1);
        return PromotionCommentResponse.from(comment);
    }

    @Transactional
    public PromotionCommentResponse createReply(UUID parentId, PromotionCommentCreateRequest request) {
        var user = currentUserProvider.requireVerifiedUser();

        var parent = commentRepository.findById(parentId);
        if (parent == null) throw new NotFoundException("Comment not found: " + parentId);

        var promotion = parent.getPromotion();
        if (promotion.getStatus() != PromotionStatus.PUBLISHED) {
            throw new NotFoundException("Promotion not published");
        }

        var now = OffsetDateTime.now();
        var reply = new PromotionCommentEntity();
        reply.setPromotion(promotion);
        reply.setParent(parent);
        reply.setClientId(user.subject());
        reply.setAuthorName(request.authorName());
        reply.setContent(request.content());
        reply.setRemoved(false);
        reply.setCreatedAt(now);
        reply.setUpdatedAt(now);
        commentRepository.persist(reply);

        promotion.setCommentsCount(promotion.getCommentsCount() + 1);
        return PromotionCommentResponse.from(reply);
    }

    @Transactional
    public PromotionCommentResponse moderateComment(UUID id, String reason) {
        var user = currentUserProvider.currentUser();

        var comment = commentRepository.findById(id);
        if (comment == null) throw new NotFoundException("Comment not found: " + id);

        var now = OffsetDateTime.now();
        comment.setRemoved(true);
        comment.setRemovedAt(now);
        comment.setUpdatedAt(now);

        var log = new ModerationLogEntity();
        log.setTargetType("COMMENT");
        log.setTargetId(id);
        log.setAction("REMOVE");
        log.setReason(reason);
        log.setActor(user.username() != null ? user.username() : user.subject());
        log.setCreatedAt(now);
        moderationLogRepository.persist(log);

        return PromotionCommentResponse.from(comment);
    }

    private br.com.descontovivo.promotion.entity.PromotionEntity findPublished(String slug) {
        return promotionRepository.findPublishedBySlug(slug)
                .orElseThrow(() -> new NotFoundException("Published promotion not found: " + slug));
    }
}
