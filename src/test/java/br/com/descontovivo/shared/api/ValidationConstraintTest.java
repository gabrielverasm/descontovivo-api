package br.com.descontovivo.shared.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class ValidationConstraintTest {

    @Test
    @TestSecurity(user = "val-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "val-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "val@test.local"),
        @Claim(key = "preferred_username", value = "val-user")
    })
    void shouldReturn400WhenTitleExceeds180() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "%s",
                    "url": "https://example.com/t",
                    "description": "Valid",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/t.jpg",
                    "storeSlug": "amazon"
                }
            """.formatted("A".repeat(181)))
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "val-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "val-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "val@test.local"),
        @Claim(key = "preferred_username", value = "val-user")
    })
    void shouldReturn400WhenDescriptionExceeds2000() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Valid Title",
                    "url": "https://example.com/t",
                    "description": "%s",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/t.jpg",
                    "storeSlug": "amazon"
                }
            """.formatted("B".repeat(2001)))
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "val-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "val-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "val@test.local"),
        @Claim(key = "preferred_username", value = "val-user")
    })
    void shouldReturn400WhenCommentContentExceeds2000() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "content": "%s" }
            """.formatted("C".repeat(2001)))
            .when().post("/api/v1/promotions/any-slug/comments")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn400WhenCommentModerationReasonEmpty() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "" }
            """)
            .when().patch("/api/v1/moderation/comments/00000000-0000-0000-0000-000000000001")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn400WhenCommentModerationReasonExceeds500() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "%s" }
            """.formatted("D".repeat(501)))
            .when().patch("/api/v1/moderation/comments/00000000-0000-0000-0000-000000000001")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn400WhenModerationActionReasonExceeds500() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "%s" }
            """.formatted("E".repeat(501)))
            .when().patch("/api/v1/moderation/promotions/00000000-0000-0000-0000-000000000001")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }

    @Test
    @TestSecurity(user = "val-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "val-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "val@test.local"),
        @Claim(key = "preferred_username", value = "val-user")
    })
    void shouldReturn400WhenVoteTypeExceeds20() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "%s" }
            """.formatted("F".repeat(21)))
            .when().put("/api/v1/promotions/any-slug/vote")
            .then()
            .statusCode(400)
            .body("status", is(400));
    }
}
