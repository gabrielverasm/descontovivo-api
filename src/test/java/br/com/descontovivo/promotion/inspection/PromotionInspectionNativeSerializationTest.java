package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.annotations.RegisterForReflection;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class PromotionInspectionNativeSerializationTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void registersJacksonDtosForNativeReflection() {
        assertRegistered(PromotionInspectionResponse.class);
        assertRegistered(PromotionInspectionResource.ErrorResponse.class);
        assertRegistered(PromotionInspectionResource.UnexpectedErrorResponse.class);
        assertRegistered(PromotionInspectionRequest.class);
        assertRegistered(InternalShopeeInspectionResponse.class);
    }

    @Test
    void serializesControlledErrorResponse() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                new PromotionInspectionResource.ErrorResponse("INVALID_URL", "URL inválida")));

        assertEquals("INVALID_URL", json.get("code").asText());
        assertEquals("URL inválida", json.get("message").asText());
        assertEquals(2, json.size());
    }

    @Test
    void serializesUnexpectedErrorResponse() throws Exception {
        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(
                new PromotionInspectionResource.UnexpectedErrorResponse(
                        "INTERNAL_ERROR", "Não foi possível processar a inspeção", "request-123")));

        assertEquals("INTERNAL_ERROR", json.get("code").asText());
        assertEquals("Não foi possível processar a inspeção", json.get("message").asText());
        assertEquals("request-123", json.get("requestId").asText());
        assertEquals(3, json.size());
    }

    @Test
    void serializesSuccessResponseWithCurrentFields() throws Exception {
        PromotionInspectionResponse response = new PromotionInspectionResponse(
                MarketplaceCode.SHOPEE, true, "input-url", "product-url", "affiliate-url",
                "Produto", new BigDecimal("99.90"), new BigDecimal("129.90"), "image-key",
                "image-url", "Loja", "Vendedor", "Loja", "Shopee", 42L, 4.8, 4.9,
                true, true, "Eletrônicos", List.of("Loja oficial"), List.of(), List.of("Aviso"));

        JsonNode json = objectMapper.readTree(objectMapper.writeValueAsString(response));

        assertEquals("SHOPEE", json.get("marketplace").asText());
        assertEquals(true, json.get("supported").asBoolean());
        assertEquals("input-url", json.get("inputUrl").asText());
        assertEquals("product-url", json.get("productUrl").asText());
        assertEquals("affiliate-url", json.get("affiliateUrl").asText());
        assertEquals("Produto", json.get("title").asText());
        assertEquals(0, new BigDecimal("99.90").compareTo(json.get("currentPrice").decimalValue()));
        assertEquals(0, new BigDecimal("129.90").compareTo(json.get("originalPrice").decimalValue()));
        assertEquals("image-key", json.get("imageKey").asText());
        assertEquals("image-url", json.get("imageUrl").asText());
        assertEquals("Loja", json.get("storeName").asText());
        assertEquals("Vendedor", json.get("sellerName").asText());
        assertEquals("Loja", json.get("soldBy").asText());
        assertEquals("Shopee", json.get("deliveredBy").asText());
        assertEquals(42L, json.get("salesCount").asLong());
        assertEquals(4.8, json.get("productRating").asDouble());
        assertEquals(4.9, json.get("sellerRating").asDouble());
        assertEquals(true, json.get("officialStore").asBoolean());
        assertEquals(true, json.get("shopeeGuarantee").asBoolean());
        assertEquals("Eletrônicos", json.get("category").asText());
        assertEquals(List.of("Loja oficial"), objectMapper.convertValue(json.get("trustSignals"), List.class));
        assertEquals(List.of(), objectMapper.convertValue(json.get("missingFields"), List.class));
        assertEquals(List.of("Aviso"), objectMapper.convertValue(json.get("warnings"), List.class));
        assertEquals(23, json.size());
    }

    @Test
    void deserializesInspectionRequests() throws Exception {
        PromotionInspectionRequest request = objectMapper.readValue(
                "{\"url\":\"https://shopee.com.br/product\"}", PromotionInspectionRequest.class);
        InternalShopeeInspectionResponse importerResponse = objectMapper.readValue(
                "{\"marketplace\":\"SHOPEE\",\"inputUrl\":\"input-url\"}",
                InternalShopeeInspectionResponse.class);

        assertEquals("https://shopee.com.br/product", request.url());
        assertEquals("SHOPEE", importerResponse.marketplace());
        assertEquals("input-url", importerResponse.inputUrl());
    }

    private static void assertRegistered(Class<?> type) {
        assertNotNull(type.getAnnotation(RegisterForReflection.class),
                () -> type.getSimpleName() + " must be registered for native reflection");
    }
}
