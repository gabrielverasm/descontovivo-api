package br.com.descontovivo.notification.api;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MediaType;
import org.jboss.resteasy.reactive.server.ServerResponseFilter;

/**
 * Adds SSE-specific headers to event-stream responses.
 *
 * <ul>
 *   <li>{@code Cache-Control: no-cache} — prevents proxies from caching the stream.</li>
 *   <li>{@code X-Accel-Buffering: no} — disables buffering in nginx/reverse proxies.</li>
 * </ul>
 *
 * <p>Only applies when Content-Type is {@code text/event-stream}.
 */
public class SseResponseFilter {

    @ServerResponseFilter
    public void addSseHeaders(ContainerRequestContext request, ContainerResponseContext response) {
        MediaType contentType = response.getMediaType();
        if (contentType != null && "text".equals(contentType.getType())
                && "event-stream".equals(contentType.getSubtype())) {
            response.getHeaders().putSingle("Cache-Control", "no-cache");
            response.getHeaders().putSingle("X-Accel-Buffering", "no");
        }
    }
}
