package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.Optional;

@ApplicationScoped
public class ShopeeAffiliateOperationService {
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String token;
    private final HttpClient client;
    private final Duration timeout;

    public ShopeeAffiliateOperationService(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "shopee.importer.base-url") String baseUrl,
            @ConfigProperty(name = "shopee.importer.token") Optional<String> token,
            @ConfigProperty(name = "shopee.importer.connect-timeout", defaultValue = "3S") Duration connectTimeout,
            @ConfigProperty(name = "shopee.importer.read-timeout", defaultValue = "12S") Duration timeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.token = token.orElse("");
        this.timeout = timeout;
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    public JsonNode execute(ShopeeAffiliateOperationRequest request) {
        if (token.isBlank()) {
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Internal importer token is not configured");
        }
        try {
            String body = objectMapper.writeValueAsString(Map.of(
                    "operation", request.operation(),
                    "keyword", Optional.ofNullable(request.keyword()).orElse(""),
                    "page", request.page(),
                    "limit", request.limit(),
                    "startTime", Optional.ofNullable(request.startTime()).orElse(0L),
                    "endTime", Optional.ofNullable(request.endTime()).orElse(0L),
                    "purchaseStatus", Optional.ofNullable(request.purchaseStatus()).orElse(-1),
                    "feedId", Optional.ofNullable(request.feedId()).orElse(0L),
                    "originUrl", Optional.ofNullable(request.originUrl()).orElse(""),
                    "subIds", request.subIds()));
            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/internal/v1/shopee/affiliate-operation"))
                    .timeout(timeout)
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(httpRequest, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() >= 500 || response.statusCode() == 401 || response.statusCode() == 403) {
                    throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Shopee importer unavailable");
                }
                JsonNode detail = objectMapper.readTree(response.body()).path("detail");
                throw new MarketplaceInspectionException(
                        detail.path("code").asText("AFFILIATE_API_REQUEST_FAILED"),
                        detail.path("message").asText("Shopee rejected the operation"));
            }
            return objectMapper.readTree(response.body());
        } catch (MarketplaceInspectionException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Shopee importer timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Shopee importer request interrupted");
        } catch (Exception e) {
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Shopee importer unavailable");
        }
    }

    public record ShopeeAffiliateOperationRequest(
            String operation,
            String keyword,
            int page,
            int limit,
            Long startTime,
            Long endTime,
            Integer purchaseStatus,
            Long feedId,
            String originUrl,
            java.util.List<String> subIds) {
        public ShopeeAffiliateOperationRequest {
            subIds = subIds == null ? java.util.List.of() : java.util.List.copyOf(subIds);
        }
    }
}
