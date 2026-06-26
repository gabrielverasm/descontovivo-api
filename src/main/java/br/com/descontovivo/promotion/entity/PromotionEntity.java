package br.com.descontovivo.promotion.entity;

import br.com.descontovivo.store.entity.StoreEntity;
import jakarta.persistence.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "promotion")
public class PromotionEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true)
    private String slug;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String url;

    @Column(name = "normalized_url", nullable = false, columnDefinition = "TEXT")
    private String normalizedUrl;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String description;

    @Column(name = "normalized_description", nullable = false, columnDefinition = "TEXT")
    private String normalizedDescription;

    @Column(name = "current_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal currentPrice;

    @Column(name = "original_price", precision = 12, scale = 2)
    private BigDecimal originalPrice;

    @Column(name = "coupon_code", length = 100)
    private String couponCode;

    @Column(name = "image_url", nullable = false, columnDefinition = "TEXT")
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PromotionStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OfferAvailability availability;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "store_id", nullable = false)
    private StoreEntity store;

    @Column(name = "created_date", nullable = false)
    private LocalDate createdDate;

    @Column(name = "created_at", nullable = false)
    private OffsetDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private OffsetDateTime updatedAt;

    @Column(name = "published_at")
    private OffsetDateTime publishedAt;

    @Column(name = "rejected_at")
    private OffsetDateTime rejectedAt;

    @Column(name = "removed_at")
    private OffsetDateTime removedAt;

    @Column(name = "publish_at", nullable = false)
    private OffsetDateTime publishAt;

    @Column(name = "verified_at")
    private OffsetDateTime verifiedAt;

    @Column(length = 50)
    private String source;

    @Column(name = "source_id", length = 200)
    private String sourceId;

    @Column(name = "batch_id", length = 200)
    private String batchId;

    @Column(name = "author_username", length = 100)
    private String authorUsername;

    @Column(length = 50)
    private String marketplace;

    @Column(name = "seller_name", length = 100)
    private String sellerName;

    @Column(name = "sold_by", length = 100)
    private String soldBy;

    @Column(name = "delivered_by", length = 100)
    private String deliveredBy;

    @Column(length = 50)
    private String category;

    @Column(name = "likes_count", nullable = false)
    private int likesCount;

    @Column(name = "dislikes_count", nullable = false)
    private int dislikesCount;

    @Column(name = "comments_count", nullable = false)
    private int commentsCount;

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    public String getSlug() { return slug; }
    public void setSlug(String slug) { this.slug = slug; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public String getNormalizedUrl() { return normalizedUrl; }
    public void setNormalizedUrl(String normalizedUrl) { this.normalizedUrl = normalizedUrl; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getNormalizedDescription() { return normalizedDescription; }
    public void setNormalizedDescription(String normalizedDescription) { this.normalizedDescription = normalizedDescription; }
    public BigDecimal getCurrentPrice() { return currentPrice; }
    public void setCurrentPrice(BigDecimal currentPrice) { this.currentPrice = currentPrice; }
    public BigDecimal getOriginalPrice() { return originalPrice; }
    public void setOriginalPrice(BigDecimal originalPrice) { this.originalPrice = originalPrice; }
    public String getCouponCode() { return couponCode; }
    public void setCouponCode(String couponCode) { this.couponCode = couponCode; }
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    public PromotionStatus getStatus() { return status; }
    public void setStatus(PromotionStatus status) { this.status = status; }
    public OfferAvailability getAvailability() { return availability; }
    public void setAvailability(OfferAvailability availability) { this.availability = availability; }
    public StoreEntity getStore() { return store; }
    public void setStore(StoreEntity store) { this.store = store; }
    public LocalDate getCreatedDate() { return createdDate; }
    public void setCreatedDate(LocalDate createdDate) { this.createdDate = createdDate; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(OffsetDateTime createdAt) { this.createdAt = createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(OffsetDateTime updatedAt) { this.updatedAt = updatedAt; }
    public OffsetDateTime getPublishedAt() { return publishedAt; }
    public void setPublishedAt(OffsetDateTime publishedAt) { this.publishedAt = publishedAt; }
    public OffsetDateTime getRejectedAt() { return rejectedAt; }
    public void setRejectedAt(OffsetDateTime rejectedAt) { this.rejectedAt = rejectedAt; }
    public OffsetDateTime getRemovedAt() { return removedAt; }
    public void setRemovedAt(OffsetDateTime removedAt) { this.removedAt = removedAt; }
    public int getLikesCount() { return likesCount; }
    public void setLikesCount(int likesCount) { this.likesCount = likesCount; }
    public int getDislikesCount() { return dislikesCount; }
    public void setDislikesCount(int dislikesCount) { this.dislikesCount = dislikesCount; }
    public int getCommentsCount() { return commentsCount; }
    public void setCommentsCount(int commentsCount) { this.commentsCount = commentsCount; }
    public OffsetDateTime getPublishAt() { return publishAt; }
    public void setPublishAt(OffsetDateTime publishAt) { this.publishAt = publishAt; }
    public OffsetDateTime getVerifiedAt() { return verifiedAt; }
    public void setVerifiedAt(OffsetDateTime verifiedAt) { this.verifiedAt = verifiedAt; }
    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }
    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }
    public String getBatchId() { return batchId; }
    public void setBatchId(String batchId) { this.batchId = batchId; }
    public String getAuthorUsername() { return authorUsername; }
    public void setAuthorUsername(String authorUsername) { this.authorUsername = authorUsername; }
    public String getMarketplace() { return marketplace; }
    public void setMarketplace(String marketplace) { this.marketplace = marketplace; }
    public String getSellerName() { return sellerName; }
    public void setSellerName(String sellerName) { this.sellerName = sellerName; }
    public String getSoldBy() { return soldBy; }
    public void setSoldBy(String soldBy) { this.soldBy = soldBy; }
    public String getDeliveredBy() { return deliveredBy; }
    public void setDeliveredBy(String deliveredBy) { this.deliveredBy = deliveredBy; }
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
}
