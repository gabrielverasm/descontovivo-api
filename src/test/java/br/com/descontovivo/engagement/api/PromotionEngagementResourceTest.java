package br.com.descontovivo.engagement.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PromotionEngagementResourceTest {

    private static String publishedSlug;
    private static String commentId;

    private static String createAndApprove() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        var json = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Eng Test %s",
                    "url": "https://www.amazon.com.br/eng-%s",
                    "description": "Engagement test %s",
                    "currentPrice": 100.00,
                    "originalPrice": 200.00,
                    "imageUrl": "https://images.example.com/eng.jpg",
                    "imageKey": "temp/promotions/2026/06/eng-%s.webp",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath();

        var id = json.getString("id");
        var slug = json.getString("slug");

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "Test" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        return slug;
    }

    @Test
    @Order(1)
    void shouldReturn401WhenVotingWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "LIKE" }
            """)
            .when().put("/api/v1/promotions/any-slug/vote")
            .then().statusCode(401);
    }

    @Test
    @Order(2)
    void shouldReturn401WhenCommentingWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "authorName": "Test", "content": "Test" }
            """)
            .when().post("/api/v1/promotions/any-slug/comments")
            .then().statusCode(401);
    }

    @Test
    @Order(3)
    @TestSecurity(user = "voter-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "voter-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "voter@test.local"),
        @Claim(key = "preferred_username", value = "voter")
    })
    void shouldVoteLikeAndIncrementCount() {
        publishedSlug = createAndApprove();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "LIKE" }
            """)
            .when().put("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(1))
            .body("dislikesCount", is(0))
            .body("userVote", is("LIKE"));
    }

    @Test
    @Order(4)
    @TestSecurity(user = "voter-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "voter-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "voter@test.local"),
        @Claim(key = "preferred_username", value = "voter")
    })
    void shouldSwitchVoteFromLikeToDislike() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "DISLIKE" }
            """)
            .when().put("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(0))
            .body("dislikesCount", is(1))
            .body("userVote", is("DISLIKE"));
    }

    @Test
    @Order(5)
    @TestSecurity(user = "voter-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "voter-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "voter@test.local"),
        @Claim(key = "preferred_username", value = "voter")
    })
    void shouldDeleteVoteAndZeroCounts() {
        given()
            .when().delete("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(0))
            .body("dislikesCount", is(0))
            .body("userVote", nullValue());
    }

    @Test
    @Order(6)
    @TestSecurity(user = "commenter-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "commenter-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "commenter@test.local"),
        @Claim(key = "preferred_username", value = "commenter")
    })
    void shouldCreateCommentOnPublishedPromotion() {
        commentId = given()
            .contentType(ContentType.JSON)
            .body("""
                { "authorName": "Gabriel", "content": "Ótima promoção!" }
            """)
            .when().post("/api/v1/promotions/" + publishedSlug + "/comments")
            .then()
            .statusCode(201)
            .body("authorName", is("Gabriel"))
            .body("content", is("Ótima promoção!"))
            .body("removed", is(false))
            .extract().jsonPath().getString("id");
    }

    @Test
    @Order(7)
    @TestSecurity(user = "replier-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "replier-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "email", value = "replier@test.local"),
        @Claim(key = "preferred_username", value = "replier")
    })
    void shouldReplyToComment() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "authorName": "Ana", "content": "Concordo!" }
            """)
            .when().post("/api/v1/comments/" + commentId + "/replies")
            .then()
            .statusCode(201)
            .body("parentId", is(commentId))
            .body("authorName", is("Ana"));
    }

    @Test
    @Order(8)
    void shouldListCommentsWithoutAuth() {
        given()
            .when().get("/api/v1/promotions/" + publishedSlug + "/comments")
            .then()
            .statusCode(200)
            .body("size()", is(2));
    }

    @Test
    @Order(9)
    @TestSecurity(user = "mod-sub", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "moderator")
    })
    void shouldModerateCommentWithModeratorRole() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "Comentário inadequado" }
            """)
            .when().patch("/api/v1/moderation/comments/" + commentId)
            .then()
            .statusCode(200)
            .body("removed", is(true))
            .body("content", is("Comentário removido"));
    }

    @Test
    @Order(10)
    void shouldReturn401WhenModeratingCommentWithoutAuth() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "Test" }
            """)
            .when().patch("/api/v1/moderation/comments/" + UUID.randomUUID())
            .then().statusCode(401);
    }
}
