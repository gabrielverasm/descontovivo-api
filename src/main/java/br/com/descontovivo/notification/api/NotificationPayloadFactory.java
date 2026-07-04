package br.com.descontovivo.notification.api;

import br.com.descontovivo.notification.dto.AdminDataRequestSnapshot;
import br.com.descontovivo.notification.dto.ModerationPromotionSnapshot;
import br.com.descontovivo.notification.dto.PublicPromotionSnapshot;
import br.com.descontovivo.notification.service.NotificationSnapshotService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Builds JSON payloads for SSE notification events.
 *
 * <p>Extracted from {@link NotificationStreamResource} so payload generation
 * can be unit-tested without opening an infinite SSE stream.
 */
@ApplicationScoped
public class NotificationPayloadFactory {

    private final NotificationSnapshotService snapshotService;
    private final ObjectMapper objectMapper;

    public NotificationPayloadFactory(NotificationSnapshotService snapshotService,
                                      ObjectMapper objectMapper) {
        this.snapshotService = snapshotService;
        this.objectMapper = objectMapper;
    }

    /**
     * Heartbeat payload with current timestamp.
     * Uses LinkedHashMap (not Map.of) because timestamps are always non-null,
     * but we keep the pattern consistent for safety.
     */
    public String buildHeartbeatPayload() {
        Map<String, String> data = new LinkedHashMap<>();
        data.put("timestamp", OffsetDateTime.now().toString());
        return toJson(data);
    }

    /**
     * Public promotions payload.
     * Serializes the record directly — Jackson handles null latestPublishedAt.
     * NEVER use Map.of here because latestPublishedAt can be null.
     */
    public String buildPublicPromotionsPayload() {
        PublicPromotionSnapshot snapshot = snapshotService.publicPromotionSnapshot();
        return toJson(snapshot);
    }

    /**
     * Moderation promotions payload.
     */
    public String buildModerationPromotionsPayload() {
        ModerationPromotionSnapshot snapshot = snapshotService.moderationPromotionSnapshot();
        return toJson(snapshot);
    }

    /**
     * Admin data-requests payload.
     */
    public String buildAdminDataRequestsPayload() {
        AdminDataRequestSnapshot snapshot = snapshotService.adminDataRequestSnapshot();
        return toJson(snapshot);
    }

    /**
     * Serialize any object to JSON.
     *
     * <p>Throws {@link IllegalStateException} on serialization failure instead of
     * silently returning {@code "{}"}. This ensures serialization problems (e.g., missing
     * reflection metadata in native image) are immediately visible rather than producing
     * empty payloads that are hard to diagnose.
     */
    String toJson(Object obj) {
        try {
            return objectMapper.writeValueAsString(obj);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize SSE payload: " + obj.getClass().getName(), e);
        }
    }
}
