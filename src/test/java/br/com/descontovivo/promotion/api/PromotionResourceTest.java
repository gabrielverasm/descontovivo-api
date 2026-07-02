package br.com.descontovivo.promotion.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
@QuarkusTest
class PromotionResourceTest {

    @Test
    void shouldListPromotionsWithoutAuth() {
        given()
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin-url-test", roles = {"user", "moderator", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-url-test-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "admin-url@test.local"),
        @Claim(key = "preferred_username", value = "admin-url-test")
    })
    void shouldReturnUrlInPromotionList() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var expectedUrl = "https://www.amazon.com.br/list-url-" + uid;

        // Create and approve a promotion
        var id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "URL List Test %s",
                    "url": "%s",
                    "currentPrice": 49.90,
                    "imageKey": "temp/promotions/2026/06/url-list-%s.webp",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, expectedUrl, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .contentType(ContentType.JSON)
            .body("{\"action\": \"APPROVE\", \"reason\": \"ok\"}")
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        // Verify url in list
        given()
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.url", hasItem(expectedUrl));
    }

    @Test
    @TestSecurity(user = "admin-url-detail", roles = {"user", "moderator", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-url-detail-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "admin-url-detail@test.local"),
        @Claim(key = "preferred_username", value = "admin-url-detail")
    })
    void shouldReturnUrlInPromotionDetail() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var expectedUrl = "https://www.amazon.com.br/detail-url-" + uid;

        // Create and approve
        var response = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "URL Detail Test %s",
                    "url": "%s",
                    "currentPrice": 79.90,
                    "imageKey": "temp/promotions/2026/06/url-detail-%s.webp",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, expectedUrl, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath();

        var id = response.getString("id");
        var slug = response.getString("slug");

        given()
            .contentType(ContentType.JSON)
            .body("{\"action\": \"APPROVE\", \"reason\": \"ok\"}")
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        // Verify url in detail
        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(200)
            .body("url", is(expectedUrl));
    }

    @Test
    void shouldReturn401WhenCreatingWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Test",
                    "url": "https://example.com/test",
                    "currentPrice": 10.00,
                    "imageKey": "temp/promotions/2026/06/test.webp"
                }
            """)
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "user-unverified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-unverified-sub"),
        @Claim(key = "email_verified", value = "false", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "unverified@test.local"),
        @Claim(key = "preferred_username", value = "user-unverified")
    })
    void shouldReturn403WhenEmailNotVerified() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Unverified Test",
                    "url": "https://example.com/unverified",
                    "currentPrice": 10.00,
                    "imageKey": "temp/promotions/2026/06/unverified.webp"
                }
            """)
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(403);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldCreatePromotionWithMinimalRequest() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Produto Mínimo %s",
                    "url": "https://www.amazon.com.br/minimal-%s",
                    "currentPrice": 99.90,
                    "imageKey": "temp/promotions/2026/06/%s.webp"
                }
            """.formatted(uid, uid, uid))

            .when().post("/api/v1/promotions")
            .then()
            .statusCode(201)
            .body("status", is("PENDING_REVIEW"))
            .body("slug", notNullValue())
            .body("id", notNullValue())
            .body("store.slug", is("amazon"))
            .body("authorUsername", is("user-verified"))
            .body("imageUrl", startsWith("https://img.descontovivo.com.br/promotions/"))
            .body("imageUrl", not(containsString("temp/promotions")));
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldCreatePromotionWithStoreSlug() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Auth Test %s",
                    "url": "https://www.example.com/auth-%s",
                    "currentPrice": 199.00,
                    "originalPrice": 399.00,
                    "imageKey": "temp/promotions/2026/06/auth-%s.webp",
                    "storeSlug": "magalu"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(201)
            .body("status", is("PENDING_REVIEW"))
            .body("store.slug", is("magalu"));
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldUseFallbackStoreWhenSlugOmittedAndUrlUnrecognized() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Unknown Store %s",
                    "url": "https://www.lojadesconhecida.com.br/item-%s",
                    "currentPrice": 50.00,
                    "imageKey": "temp/promotions/2026/06/unknown-%s.webp"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(201)
            .body("store.slug", is("loja-nao-identificada"));
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn409OnDuplicateUrlSameDay() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var body = """
            {
                "title": "Dup %s",
                "url": "https://www.magalu.com.br/dup-%s",
                "currentPrice": 100.00,
                "imageKey": "temp/promotions/2026/06/dup-%s.webp",
                "storeSlug": "magalu"
            }
        """.formatted(uid, uid, uid);

        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/v1/promotions")
            .then().statusCode(201);

        given().contentType(ContentType.JSON).body(body)
            .when().post("/api/v1/promotions")
            .then().statusCode(409);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldFailWithNonExistentStore() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Fake Store",
                    "url": "https://www.fake.com.br/item",
                    "currentPrice": 10.00,
                    "imageKey": "temp/promotions/2026/06/fake.webp",
                    "storeSlug": "loja-fantasma"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(404);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn400WhenTitleMissing() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "url": "https://www.amazon.com.br/item",
                    "currentPrice": 10.00
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn400WhenUrlMissing() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "No URL",
                    "currentPrice": 10.00
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn400WhenCurrentPriceMissing() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "No Price",
                    "url": "https://www.amazon.com.br/item"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn400WhenImageKeyMissing() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "No Key",
                    "url": "https://www.amazon.com.br/item",
                    "currentPrice": 10.00
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(400);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn422WhenImageKeyNotInTempPrefix() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Invalid Key",
                    "url": "https://www.amazon.com.br/invalid-key",
                    "currentPrice": 10.00,
                    "imageKey": "promotions/2026/06/a.webp"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(422);
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn422WhenImageKeyHasArbitraryPrefix() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Arbitrary Key",
                    "url": "https://www.amazon.com.br/arbitrary-key",
                    "currentPrice": 10.00,
                    "imageKey": "abc/a.webp"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(422);
    }
}
