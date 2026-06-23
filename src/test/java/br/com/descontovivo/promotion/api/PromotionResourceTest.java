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
    void shouldReturn401WhenCreatingWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Test",
                    "url": "https://example.com/test",
                    "description": "Test desc",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/test.jpg",
                    "storeSlug": "amazon"
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
                    "description": "Unverified user test",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/test.jpg",
                    "storeSlug": "amazon"
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
    void shouldCreatePromotionWhenAuthenticated() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Auth Test %s",
                    "url": "https://www.amazon.com.br/auth-%s",
                    "description": "Auth test %s",
                    "currentPrice": 199.00,
                    "originalPrice": 399.00,
                    "imageUrl": "https://images.example.com/auth.jpg",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(201)
            .body("status", is("PENDING_REVIEW"))
            .body("slug", notNullValue())
            .body("id", notNullValue());
    }

    @Test
    @TestSecurity(user = "user-verified", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-verified-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "user@test.local"),
        @Claim(key = "preferred_username", value = "user-verified")
    })
    void shouldReturn409OnDuplicateSameDay() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var body = """
            {
                "title": "Dup %s",
                "url": "https://www.magalu.com.br/dup-%s",
                "description": "Dup desc %s",
                "currentPrice": 100.00,
                "imageUrl": "https://images.example.com/dup.jpg",
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
                    "description": "Non-existent store",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/fake.jpg",
                    "storeSlug": "loja-fantasma"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(404);
    }
}
