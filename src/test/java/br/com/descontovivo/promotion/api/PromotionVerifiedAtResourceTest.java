package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.repository.PromotionRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class PromotionVerifiedAtResourceTest {
    @Inject PromotionRepository promotions;

    @Test
    @TestSecurity(user = "verified-at-admin", roles = {"admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "verified-at-admin-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "verified-at-admin")
    })
    void publicDetailAndListExposeVerifiedAtAndKeepExistingFields() {
        String source = "verified-" + UUID.randomUUID().toString().substring(0, 8);
        given().contentType(ContentType.JSON).body("""
            { "items": [{ "sourceId": "%s", "title": "Verified At %s", "marketplace": "AMAZON",
                "storeName": "Amazon", "productUrl": "https://example.com/%s",
                "imageUrl": "https://images.example.com/test.jpg", "currentPrice": 10.00,
                "verifiedAt": "2026-09-01T12:00:00Z" }] }
            """.formatted(source, source, source))
            .when().post("/api/v1/admin/promotions/import").then().statusCode(200).body("created", is(1));
        String slug = QuarkusTransaction.requiringNew().call(() -> promotions.find("sourceId", source).firstResult().getSlug());

        given().when().get("/api/v1/promotions/" + slug)
            .then().statusCode(200)
            .body("verifiedAt", startsWith("2026-09-01T12:00:00"))
            .body("slug", is(slug))
            .body("url", is("https://example.com/" + source))
            .body("trustSignals", notNullValue());

        given().when().get("/api/v1/promotions?size=100")
            .then().statusCode(200)
            .body("content.find { it.slug == '" + slug + "' }.verifiedAt", startsWith("2026-09-01T12:00:00"));
    }
}
