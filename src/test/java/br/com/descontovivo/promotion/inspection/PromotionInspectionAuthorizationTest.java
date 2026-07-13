package br.com.descontovivo.promotion.inspection;

import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@QuarkusTest
class PromotionInspectionAuthorizationTest {
    @InjectMock PromotionInspectionService service;

    @BeforeEach void setup() {
        when(service.inspect(anyString())).thenReturn(new PromotionInspectionResponse(
                MarketplaceCode.SHOPEE, true, "input", "product", null, "Title",
                BigDecimal.TEN, null, null, null, null, null, null, null,
                null, null, null, false, false, null, List.of(), List.of(), List.of()));
    }

    @Test void unauthenticatedIsRejected() {
        given().contentType("application/json").body("{\"url\":\"https://shopee.com.br/x\"}")
                .when().post("/api/v1/admin/promotions/inspect-url").then().statusCode(401);
    }

    @Test @TestSecurity(user = "user", roles = "user")
    void regularUserIsForbidden() {
        given().contentType("application/json").body("{\"url\":\"https://shopee.com.br/x\"}")
                .when().post("/api/v1/admin/promotions/inspect-url").then().statusCode(403);
    }

    @Test @TestSecurity(user = "moderator", roles = "moderator")
    void moderatorIsAllowed() {
        given().contentType("application/json").body("{\"url\":\"https://shopee.com.br/x\"}")
                .when().post("/api/v1/admin/promotions/inspect-url")
                .then().statusCode(200).body("marketplace", is("SHOPEE"));
    }

    @Test @TestSecurity(user = "admin", roles = "admin")
    void adminIsAllowed() {
        given().contentType("application/json").body("{\"url\":\"https://shopee.com.br/x\"}")
                .when().post("/api/v1/admin/promotions/inspect-url")
                .then().statusCode(200).body("marketplace", is("SHOPEE"));
    }
}
