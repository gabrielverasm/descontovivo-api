package br.com.descontovivo.engagement.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.*;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class PromotionEngagementResourceTest {

    private static final String ADMIN_TOKEN = "test-admin-token";
    private static String publishedSlug;
    private static String pendingSlug;
    private static String commentId;

    private static String[] createAndApprove() {
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
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath();

        var id = json.getString("id");
        var slug = json.getString("slug");

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", ADMIN_TOKEN)
            .body("""
                { "action": "APPROVE", "reason": "Test" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        return new String[]{id, slug};
    }

    private static String createPending() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Pending %s",
                    "url": "https://www.amazon.com.br/pend-%s",
                    "description": "Pending test %s",
                    "currentPrice": 50.00,
                    "imageUrl": "https://images.example.com/pend.jpg",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("slug");
    }

    @Test
    @Order(1)
    void shouldVoteLikeAndIncrementCount() {
        var result = createAndApprove();
        publishedSlug = result[1];

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-1", "type": "LIKE" }
            """)
            .when().put("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(1))
            .body("dislikesCount", is(0))
            .body("userVote", is("LIKE"));
    }

    @Test
    @Order(2)
    void shouldSwitchVoteFromLikeToDislike() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-1", "type": "DISLIKE" }
            """)
            .when().put("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(0))
            .body("dislikesCount", is(1))
            .body("userVote", is("DISLIKE"));
    }

    @Test
    @Order(3)
    void shouldDeleteVoteAndZeroCounts() {
        given()
            .queryParam("clientId", "client-1")
            .when().delete("/api/v1/promotions/" + publishedSlug + "/vote")
            .then()
            .statusCode(200)
            .body("likesCount", is(0))
            .body("dislikesCount", is(0))
            .body("userVote", nullValue());
    }

    @Test
    @Order(4)
    void shouldCreateCommentOnPublishedPromotion() {
        commentId = given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-1", "authorName": "Gabriel", "content": "Ótima promoção!" }
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
    @Order(5)
    void shouldFailCommentOnPendingPromotion() {
        pendingSlug = createPending();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-1", "authorName": "Gabriel", "content": "Test" }
            """)
            .when().post("/api/v1/promotions/" + pendingSlug + "/comments")
            .then()
            .statusCode(404);
    }

    @Test
    @Order(6)
    void shouldReplyToComment() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-2", "authorName": "Ana", "content": "Concordo!" }
            """)
            .when().post("/api/v1/comments/" + commentId + "/replies")
            .then()
            .statusCode(201)
            .body("parentId", is(commentId))
            .body("authorName", is("Ana"));
    }

    @Test
    @Order(7)
    void shouldListCommentsWithRootAndReply() {
        given()
            .when().get("/api/v1/promotions/" + publishedSlug + "/comments")
            .then()
            .statusCode(200)
            .body("size()", is(2))
            .body("[0].authorName", is("Gabriel"))
            .body("[1].parentId", is(commentId));
    }

    @Test
    @Order(8)
    void shouldModerateCommentAndShowRemoved() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", ADMIN_TOKEN)
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
    @Order(9)
    void shouldShowRemovedCommentInList() {
        given()
            .when().get("/api/v1/promotions/" + publishedSlug + "/comments")
            .then()
            .statusCode(200)
            .body("[0].removed", is(true))
            .body("[0].content", is("Comentário removido"));
    }

    @Test
    @Order(10)
    void shouldShowEngagementCountsInPromotionDetail() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "clientId": "client-detail", "type": "LIKE" }
            """)
            .when().put("/api/v1/promotions/" + publishedSlug + "/vote")
            .then().statusCode(200);

        given()
            .when().get("/api/v1/promotions/" + publishedSlug)
            .then()
            .statusCode(200)
            .body("likesCount", is(1))
            .body("dislikesCount", is(0))
            .body("commentsCount", is(2));
    }
}
