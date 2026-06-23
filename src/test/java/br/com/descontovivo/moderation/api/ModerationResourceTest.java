package br.com.descontovivo.moderation.api;

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
class ModerationResourceTest {

    @Test
    void shouldReturn401WithoutAuth() {
        given()
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "regular-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "regular-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "regular-user")
    })
    void shouldReturn403ForRegularUser() {
        given()
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldAllowModeratorToListPending() {
        given()
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "moderator", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "admin-user")
    })
    void shouldAllowAdminToApprovePromotion() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "Valid offer" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("PUBLISHED"))
            .body("publishedAt", notNullValue());
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldAllowModeratorToApprovePromotion() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "Looks good" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("PUBLISHED"));
    }

    private String createPromotion() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Mod Test %s",
                    "url": "https://www.amazon.com.br/mod-%s",
                    "description": "Moderation test %s",
                    "currentPrice": 100.00,
                    "imageUrl": "https://images.example.com/mod.jpg",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("id");
    }
}
