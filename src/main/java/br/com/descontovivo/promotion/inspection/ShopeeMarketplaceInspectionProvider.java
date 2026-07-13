package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class ShopeeMarketplaceInspectionProvider implements MarketplaceInspectionProvider {
    private final ObjectMapper objectMapper;
    private final String baseUrl;
    private final String token;
    private final HttpClient client;
    private final Duration readTimeout;

    public ShopeeMarketplaceInspectionProvider(
            ObjectMapper objectMapper,
            @ConfigProperty(name = "shopee.importer.base-url") String baseUrl,
            @ConfigProperty(name = "shopee.importer.token") Optional<String> token,
            @ConfigProperty(name = "shopee.importer.connect-timeout", defaultValue = "3S") Duration connectTimeout,
            @ConfigProperty(name = "shopee.importer.read-timeout", defaultValue = "12S") Duration readTimeout) {
        this.objectMapper = objectMapper;
        this.baseUrl = baseUrl;
        this.token = token.orElse("");
        this.readTimeout = readTimeout;
        this.client = HttpClient.newBuilder().connectTimeout(connectTimeout).followRedirects(HttpClient.Redirect.NEVER).build();
    }

    @Override
    public boolean supports(MarketplaceCode marketplace) { return marketplace == MarketplaceCode.SHOPEE; }

    @Override
    public MarketplaceInspectionData inspect(String url) {
        if (token == null || token.isBlank()) {
            throw new MarketplaceInspectionException(
                    "IMPORTER_UNAVAILABLE", "Internal importer token is not configured");
        }
        try {
            String body = objectMapper.writeValueAsString(new PromotionInspectionRequest(url));
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(baseUrl + "/internal/v1/shopee/inspect"))
                    .timeout(readTimeout)
                    .header("Content-Type", "application/json")
                    .header("X-Internal-Token", token)
                    .POST(HttpRequest.BodyPublishers.ofString(body)).build();
            HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() < 200 || response.statusCode() >= 300) {
                if (response.statusCode() == 401 || response.statusCode() == 403) {
                    throw new MarketplaceInspectionException(
                            "IMPORTER_UNAVAILABLE", "Internal importer authentication failed");
                }
                if (response.statusCode() >= 500) {
                    throw new MarketplaceInspectionException(
                            "IMPORTER_UNAVAILABLE", "Importer unavailable");
                }
                var detail = objectMapper.readTree(response.body()).path("detail");
                String code = detail.path("code").asText("INSPECTION_FAILED");
                String message = detail.path("message").asText("Importer rejected the inspection");
                if (response.statusCode() != 400 && response.statusCode() != 422) {
                    code = "INSPECTION_FAILED";
                }
                throw new MarketplaceInspectionException(code, message);
            }
            InternalShopeeInspectionResponse data = objectMapper.readValue(
                    response.body(), InternalShopeeInspectionResponse.class);
            return new MarketplaceInspectionData(
                    MarketplaceCode.SHOPEE, data.inputUrl(), data.productUrl(), data.affiliateUrl(),
                    data.title(), data.currentPrice(), data.originalPrice(), data.remoteImageUrl(),
                    data.storeName(), data.sellerName(), data.soldBy(), data.deliveredBy(),
                    data.salesCount(), data.productRating(), data.sellerRating(), data.officialStore(),
                    data.shopeeGuarantee(), data.category(), data.missingFields(), data.warnings());
        } catch (MarketplaceInspectionException e) {
            throw e;
        } catch (java.net.http.HttpTimeoutException e) {
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Importer timeout");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Importer request interrupted");
        } catch (Exception e) {
            throw new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Importer unavailable");
        }
    }
}
