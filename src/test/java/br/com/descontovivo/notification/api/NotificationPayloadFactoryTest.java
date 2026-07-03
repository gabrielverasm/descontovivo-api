package br.com.descontovivo.notification.api;

import br.com.descontovivo.notification.dto.AdminDataRequestSnapshot;
import br.com.descontovivo.notification.dto.ModerationPromotionSnapshot;
import br.com.descontovivo.notification.dto.PublicPromotionSnapshot;
import br.com.descontovivo.notification.service.NotificationSnapshotService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.time.OffsetDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

/**
 * Unit tests for SSE payload generation.
 *
 * <p>Tests the JSON payloads that would be sent via SSE without opening any stream.
 * Fast, deterministic, no Quarkus startup needed.
 */
class NotificationPayloadFactoryTest {

    private NotificationSnapshotService snapshotService;
    private NotificationPayloadFactory factory;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        snapshotService = Mockito.mock(NotificationSnapshotService.class);
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        factory = new NotificationPayloadFactory(snapshotService, objectMapper);
    }

    // ─── Heartbeat ─────────────────────────────────────────────────────

    @Test
    void heartbeatPayload_containsTimestamp() throws Exception {
        String json = factory.buildHeartbeatPayload();

        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("timestamp"), "Heartbeat must contain 'timestamp' field");
        assertFalse(node.get("timestamp").asText().isBlank(), "Timestamp must not be blank");
    }

    // ─── Public promotions ─────────────────────────────────────────────

    @Test
    void publicPromotionsPayload_containsPublishedCountAndLatestPublishedAt() throws Exception {
        OffsetDateTime now = OffsetDateTime.now();
        when(snapshotService.publicPromotionSnapshot())
                .thenReturn(new PublicPromotionSnapshot(42, now));

        String json = factory.buildPublicPromotionsPayload();

        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("publishedCount"), "Must contain 'publishedCount'");
        assertEquals(42, node.get("publishedCount").asInt());
        assertTrue(node.has("latestPublishedAt"), "Must contain 'latestPublishedAt'");
        assertFalse(node.get("latestPublishedAt").isNull(), "latestPublishedAt should not be null when set");
    }

    @Test
    void publicPromotionsPayload_withNullLatestPublishedAt_doesNotThrowNPE() throws Exception {
        // This is the critical test: Map.of would NPE here, but record serialization handles null.
        when(snapshotService.publicPromotionSnapshot())
                .thenReturn(new PublicPromotionSnapshot(0, null));

        String json = factory.buildPublicPromotionsPayload();

        // Must not throw. Verify JSON is valid and contains expected fields.
        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("publishedCount"), "Must contain 'publishedCount'");
        assertEquals(0, node.get("publishedCount").asInt());
        assertTrue(node.has("latestPublishedAt"), "Must contain 'latestPublishedAt' even when null");
        assertTrue(node.get("latestPublishedAt").isNull(), "latestPublishedAt should be JSON null");
    }

    // ─── Moderation ────────────────────────────────────────────────────

    @Test
    void moderationPromotionsPayload_containsPendingCount() throws Exception {
        when(snapshotService.moderationPromotionSnapshot())
                .thenReturn(new ModerationPromotionSnapshot(7));

        String json = factory.buildModerationPromotionsPayload();

        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("pendingCount"), "Must contain 'pendingCount'");
        assertEquals(7, node.get("pendingCount").asInt());
    }

    // ─── Admin data requests ───────────────────────────────────────────

    @Test
    void adminDataRequestsPayload_containsOpenCount() throws Exception {
        when(snapshotService.adminDataRequestSnapshot())
                .thenReturn(new AdminDataRequestSnapshot(3));

        String json = factory.buildAdminDataRequestsPayload();

        JsonNode node = objectMapper.readTree(json);
        assertTrue(node.has("openCount"), "Must contain 'openCount'");
        assertEquals(3, node.get("openCount").asInt());
    }

    // ─── Map.of null regression ────────────────────────────────────────

    @Test
    void publicPromotionSnapshot_zeroCountNullDate_serializesWithoutNPE() throws Exception {
        // Direct DTO serialization test — ensures no Map.of is used internally.
        PublicPromotionSnapshot snapshot = new PublicPromotionSnapshot(0, null);
        String json = objectMapper.writeValueAsString(snapshot);

        JsonNode node = objectMapper.readTree(json);
        assertEquals(0, node.get("publishedCount").asInt());
        assertTrue(node.get("latestPublishedAt").isNull());
    }
}
