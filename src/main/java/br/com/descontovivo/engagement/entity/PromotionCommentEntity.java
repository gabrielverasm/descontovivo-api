package br.com.descontovivo.engagement.entity;

import br.com.descontovivo.promotion.entity.PromotionEntity;
import jakarta.persistence.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion_comment")
public class PromotionCommentEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "promotion_id", nullable = false)
    private PromotionEntity promotion;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_id")
    private PromotionCommentEntity parent;

    @Column(name = "client_id", nullable = false, length = 120)
    private String clientId;

    @Column(name = "author_name", nullable = false, length = 120)
    private String authorName;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(nullable = false)
    private boolean removed;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public PromotionEntity getPromotion() { return promotion; }
    public void setPromotion(PromotionEntity promotion) { this.promotion = promotion; }
    public PromotionCommentEntity getParent() { return parent; }
    public void setParent(PromotionCommentEntity parent) { this.parent = parent; }
    public String getClientId() { return clientId; }
    public void setClientId(String clientId) { this.clientId = clientId; }
    public String getAuthorName() { return authorName; }
    public void setAuthorName(String authorName) { this.authorName = authorName; }
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    public boolean isRemoved() { return removed; }
    public void setRemoved(boolean removed) { this.removed = removed; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getRemovedAt() { return removedAt; }
    public void setRemovedAt(OffsetDateTime removedAt) { this.removedAt = removedAt; }
}
