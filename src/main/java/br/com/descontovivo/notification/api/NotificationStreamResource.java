package br.com.descontovivo.notification.api;

import br.com.descontovivo.notification.config.SseConfig;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.annotation.security.PermitAll;
import jakarta.annotation.security.RolesAllowed;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.util.List;

/**
 * SSE notification stream endpoints.
 *
 * <p>Public stream: aggregate promotion data, accessible without authentication.
 * <p>Admin stream: moderation and data-request counters, requires admin role.
 *
 * <p><strong>JPA offload strategy:</strong> Hibernate ORM requires a managed thread (not Vert.x IO thread).
 * We use {@code runSubscriptionOn(workerPool)} for the initial emission and
 * {@code emitOn(workerPool)} for the periodic tick supplier. Both ensure that
 * {@link br.com.descontovivo.notification.service.NotificationSnapshotService} methods
 * (which are @Transactional) execute on the default worker pool, not the IO event loop.
 *
 * <p>Security: No sensitive data is sent on public stream. Admin stream requires Bearer token
 * via Authorization header (NOT query param).
 */
@Path("/api/v1/events")
@Tag(name = "Notifications SSE", description = "Server-Sent Events for real-time notification snapshots")
public class NotificationStreamResource {

    private final NotificationPayloadFactory payloadFactory;
    private final Duration snapshotInterval;

    public NotificationStreamResource(NotificationPayloadFactory payloadFactory,
                                      SseConfig sseConfig) {
        this.payloadFactory = payloadFactory;
        this.snapshotInterval = Duration.ofSeconds(sseConfig.intervalSeconds());
    }

    /**
     * Public SSE stream emitting promotion snapshots.
     * <p>Events: heartbeat, promotions.
     * <p>Can be consumed with native EventSource (no auth required).
     */
    @GET
    @Path("/public/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @PermitAll
    @Operation(summary = "Public notification stream",
               description = "SSE stream with periodic promotion snapshots. No authentication required.")
    public Multi<OutboundSseEvent> publicStream(@Context Sse sse) {
        return Multi.createBy().concatenating().streams(
                // Initial snapshot — offloaded to worker pool for Hibernate access.
                Multi.createFrom().item(() -> buildPublicEvents(sse))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToMultiAndConcatenate(list ->
                                Multi.createFrom().items(list.stream())),
                // Periodic snapshots — emitOn ensures the tick handler runs on worker pool.
                Multi.createFrom().ticks().every(snapshotInterval)
                        .emitOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToMultiAndConcatenate(tick ->
                                Multi.createFrom().items(buildPublicEvents(sse).stream()))
        );
    }

    /**
     * Admin SSE stream emitting moderation and data-request snapshots.
     * <p>Events: heartbeat, moderation-promotions, admin-data-requests.
     * <p>Requires admin role. Must be consumed with fetch streaming (Authorization header).
     */
    @GET
    @Path("/admin/stream")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.TEXT_PLAIN)
    @RolesAllowed({"admin"})
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Admin notification stream",
               description = "SSE stream with moderation and data-request snapshots. Requires admin role and Bearer token.")
    public Multi<OutboundSseEvent> adminStream(@Context Sse sse) {
        return Multi.createBy().concatenating().streams(
                Multi.createFrom().item(() -> buildAdminEvents(sse))
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToMultiAndConcatenate(list ->
                                Multi.createFrom().items(list.stream())),
                Multi.createFrom().ticks().every(snapshotInterval)
                        .emitOn(Infrastructure.getDefaultWorkerPool())
                        .onItem().transformToMultiAndConcatenate(tick ->
                                Multi.createFrom().items(buildAdminEvents(sse).stream()))
        );
    }

    private List<OutboundSseEvent> buildPublicEvents(Sse sse) {
        OutboundSseEvent heartbeat = sse.newEventBuilder()
                .name("heartbeat")
                .data(String.class, payloadFactory.buildHeartbeatPayload())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        OutboundSseEvent promotions = sse.newEventBuilder()
                .name("promotions")
                .data(String.class, payloadFactory.buildPublicPromotionsPayload())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        return List.of(heartbeat, promotions);
    }

    private List<OutboundSseEvent> buildAdminEvents(Sse sse) {
        OutboundSseEvent heartbeat = sse.newEventBuilder()
                .name("heartbeat")
                .data(String.class, payloadFactory.buildHeartbeatPayload())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        OutboundSseEvent moderationEvent = sse.newEventBuilder()
                .name("moderation-promotions")
                .data(String.class, payloadFactory.buildModerationPromotionsPayload())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        OutboundSseEvent dataEvent = sse.newEventBuilder()
                .name("admin-data-requests")
                .data(String.class, payloadFactory.buildAdminDataRequestsPayload())
                .mediaType(MediaType.APPLICATION_JSON_TYPE)
                .build();

        return List.of(heartbeat, moderationEvent, dataEvent);
    }
}
