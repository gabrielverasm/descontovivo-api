package br.com.descontovivo.promotion.api;

import br.com.descontovivo.promotion.inspection.AmazonShortLinkResolver;
import br.com.descontovivo.promotion.inspection.MarketplaceInspectionException;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import io.quarkus.narayana.jta.QuarkusTransaction;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

/** Garantia de link canônico da Amazon (app.amazon.enforce-tag=true) em criar, editar e importar. */
@QuarkusTest
@TestProfile(AmazonTagEnforcementResourceTest.Profile.class)
class AmazonTagEnforcementResourceTest {
    public static class Profile implements QuarkusTestProfile {
        @Override public Map<String, String> getConfigOverrides() {
            return Map.of("app.amazon.enforce-tag", "true", "admin.import.token", "amazon-enforce-token");
        }
    }

    private static final String TAG = "descontovivoo-20";

    @InjectMock AmazonShortLinkResolver resolver;
    @Inject PromotionRepository promotions;

    @BeforeEach void setUp() {
        when(resolver.isShortLink(anyString())).thenAnswer(i -> i.getArgument(0, String.class).contains("amzn.to"));
    }

    private static String uid() { return UUID.randomUUID().toString().substring(0, 8); }

    private String createBody(String url) {
        String id = uid();
        return """
            { "title": "Amazon Enforce %s", "url": "%s", "currentPrice": 99.90,
              "imageKey": "temp/promotions/2026/06/%s.webp", "storeSlug": "amazon" }
            """.formatted(id, url, id);
    }

    @Test
    @TestSecurity(user = "amazon-enforce-admin", roles = {"user", "moderator", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "amazon-enforce-admin-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "amazon-enforce@test.local"),
        @Claim(key = "preferred_username", value = "amazon-enforce-admin")
    })
    void createStoresCanonicalLinkAndEditAndShortLinksFollowTheSameRule() {
        String asin = "B0" + uid().toUpperCase().replaceAll("[^A-Z0-9]", "0").substring(0, 8);

        given().contentType(ContentType.JSON)
            .body(createBody("https://www.amazon.com.br/Produto/dp/" + asin + "?tag=descontoviv0f-20&utm_source=x&th=1"))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .body("url", is("https://www.amazon.com.br/dp/" + asin + "?tag=" + TAG + "&th=1"));

        // link curto resolvido na hora
        String asin2 = "B1" + asin.substring(2);
        when(resolver.resolve("https://amzn.to/ok")).thenReturn("https://www.amazon.com.br/dp/" + asin2);
        var id = given().contentType(ContentType.JSON).body(createBody("https://amzn.to/ok"))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .body("url", is("https://www.amazon.com.br/dp/" + asin2 + "?tag=" + TAG))
            .extract().jsonPath().getString("id");

        // edição com tag morta
        given().contentType(ContentType.JSON)
            .body("{\"action\":\"EDIT\",\"reason\":\"tag\",\"url\":\"https://www.amazon.com.br/dp/" + asin2 + "?tag=gatry0b-20\"}")
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200)
            .body("url", is("https://www.amazon.com.br/dp/" + asin2 + "?tag=" + TAG));

        // link que não é Amazon não é mexido
        given().contentType(ContentType.JSON)
            .body("{\"action\":\"EDIT\",\"reason\":\"loja\",\"url\":\"https://example.com/p/" + asin2 + "\"}")
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200)
            .body("url", is("https://example.com/p/" + asin2));
    }

    @Test
    @TestSecurity(user = "amazon-enforce-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "amazon-enforce-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "amazon-enforce-user@test.local"),
        @Claim(key = "preferred_username", value = "amazon-enforce-user")
    })
    void createRejectsUnresolvedShortLinkAndLinksWithoutAsin() {
        when(resolver.resolve("https://amzn.to/broken"))
            .thenThrow(new MarketplaceInspectionException("INSPECTION_FAILED", "timeout"));

        given().contentType(ContentType.JSON).body(createBody("https://amzn.to/broken"))
            .when().post("/api/v1/promotions")
            .then().statusCode(422)
            .body("message", containsString("SiteStripe"));

        given().contentType(ContentType.JSON).body(createBody("https://www.amazon.com.br/s?k=fone"))
            .when().post("/api/v1/promotions")
            .then().statusCode(422)
            .body("message", containsString("ASIN"));
    }

    @Test
    @TestSecurity(user = "amazon-enforce-mod", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "amazon-enforce-mod-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "amazon-enforce-mod")
    })
    void editRejectsUnresolvedShortLink() {
        var id = given().contentType(ContentType.JSON).body(createBody("https://example.com/edit-" + uid()))
            .when().post("/api/v1/promotions").then().statusCode(201).extract().jsonPath().getString("id");
        when(resolver.resolve("https://amzn.to/broken"))
            .thenThrow(new MarketplaceInspectionException("INSPECTION_FAILED", "timeout"));

        given().contentType(ContentType.JSON)
            .body("{\"action\":\"EDIT\",\"reason\":\"x\",\"url\":\"https://amzn.to/broken\"}")
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(422)
            .body("message", containsString("SiteStripe"));
    }

    private String importBody(String sourceId, String url) {
        return """
            { "batchId": "amazon-enforce", "items": [{
                "sourceId": "%s", "title": "Import Amazon %s", "marketplace": "AMAZON",
                "storeName": "Amazon", "productUrl": "%s",
                "imageUrl": "https://images.example.com/test.jpg", "currentPrice": 199.90 }] }
            """.formatted(sourceId, sourceId, url);
    }

    @Test
    void importStoresCanonicalLinkAndReportsPerItemErrors() {
        String source = "enf-" + uid();
        String asin = "B2" + uid().toUpperCase().replaceAll("[^A-Z0-9]", "0").substring(0, 8);
        given().contentType(ContentType.JSON).header("X-Admin-Import-Token", "amazon-enforce-token")
            .body(importBody(source, "https://www.amazon.com.br/dp/" + asin + "?tag=descontovivo.com-20&psc=1"))
            .when().post("/api/v1/admin/promotions/import")
            .then().statusCode(200).body("created", is(1)).body("errors", empty());

        String stored = QuarkusTransaction.requiringNew().call(() ->
                promotions.find("sourceId", source).firstResult().getUrl());
        assertEquals("https://www.amazon.com.br/dp/" + asin + "?psc=1&tag=" + TAG, stored);
        given().contentType(ContentType.JSON).header("X-Admin-Import-Token", "amazon-enforce-token")
            .queryParam("dryRun", true)
            .body(importBody("dry-" + uid(), "https://www.amazon.com.br/dp/" + asin + "?tag=x-20"))
            .when().post("/api/v1/admin/promotions/import")
            .then().statusCode(200).body("errors", empty());

        when(resolver.resolve("https://amzn.to/broken"))
            .thenThrow(new MarketplaceInspectionException("INSPECTION_FAILED", "timeout"));
        for (boolean dryRun : new boolean[]{true, false}) {
            given().contentType(ContentType.JSON).header("X-Admin-Import-Token", "amazon-enforce-token")
                .queryParam("dryRun", dryRun)
                .body(importBody("broken-" + uid(), "https://amzn.to/broken"))
                .when().post("/api/v1/admin/promotions/import")
                .then().statusCode(200)
                .body("created", is(0))
                .body("errors[0].field", is("productUrl"))
                .body("errors[0].message", containsString("SiteStripe"));
        }
    }
}
