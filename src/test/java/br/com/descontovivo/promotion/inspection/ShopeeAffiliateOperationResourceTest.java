package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@QuarkusTest
class ShopeeAffiliateOperationResourceTest {
    @InjectMock ShopeeAffiliateOperationService service;

    @Test void unauthenticatedIsRejected() {
        given().contentType("application/json").body("{\"operation\":\"validation\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation").then().statusCode(401);
    }

    @Test @TestSecurity(user = "user", roles = "user")
    void regularUserIsForbidden() {
        given().contentType("application/json").body("{\"operation\":\"validation\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation").then().statusCode(403);
    }

    @Test @TestSecurity(user = "moderator", roles = "moderator")
    void moderatorIsAllowedAndValidationIdIsForwarded() {
        when(service.execute(any())).thenReturn(JsonNodeFactory.instance.objectNode());

        given().contentType("application/json")
                .body("{\"operation\":\"validation\",\"page\":1,\"limit\":20,\"validationId\":555}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(200);

        ArgumentCaptor<ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest> captor =
                ArgumentCaptor.forClass(ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest.class);
        verify(service).execute(captor.capture());
        assertEquals(555L, captor.getValue().validationId());
    }

    @Test @TestSecurity(user = "moderator", roles = "moderator")
    void feedDataFieldsAreForwarded() {
        when(service.execute(any())).thenReturn(JsonNodeFactory.instance.objectNode());

        given().contentType("application/json")
                .body("{\"operation\":\"feed_data\",\"page\":1,\"limit\":100,"
                        + "\"datafeedId\":\"feed_FULL_2026-09-08\",\"offset\":100,\"scrollId\":\"abc\"}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(200);

        ArgumentCaptor<ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest> captor =
                ArgumentCaptor.forClass(ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest.class);
        verify(service).execute(captor.capture());
        assertEquals("feed_FULL_2026-09-08", captor.getValue().datafeedId());
        assertEquals(100L, captor.getValue().offset());
        assertEquals("abc", captor.getValue().scrollId());
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void adminIsAllowed() {
        when(service.execute(any())).thenReturn(JsonNodeFactory.instance.objectNode());

        given().contentType("application/json").body("{\"operation\":\"shops\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(200);
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void mapsInvalidOperationTo400() {
        when(service.execute(any())).thenThrow(
                new MarketplaceInspectionException("INVALID_OPERATION", "Shopee validation ID is invalid"));

        given().contentType("application/json")
                .body("{\"operation\":\"validation\",\"page\":1,\"limit\":20,\"validationId\":-1}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(400).body("code", org.hamcrest.Matchers.is("INVALID_OPERATION"));
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void mapsRateLimitTo429() {
        when(service.execute(any())).thenThrow(
                new MarketplaceInspectionException("AFFILIATE_API_RATE_LIMIT", "rate limited"));

        given().contentType("application/json").body("{\"operation\":\"shops\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(429);
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void mapsImporterUnavailableTo503() {
        when(service.execute(any())).thenThrow(
                new MarketplaceInspectionException("IMPORTER_UNAVAILABLE", "Shopee importer unavailable"));

        given().contentType("application/json").body("{\"operation\":\"shops\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(503);
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void mapsUnknownCodeTo502() {
        when(service.execute(any())).thenThrow(
                new MarketplaceInspectionException("AFFILIATE_API_REQUEST_FAILED", "schema error"));

        given().contentType("application/json").body("{\"operation\":\"shops\",\"page\":1,\"limit\":20}")
                .when().post("/api/v1/admin/shopee/affiliate-operation")
                .then().statusCode(502);
    }
}
