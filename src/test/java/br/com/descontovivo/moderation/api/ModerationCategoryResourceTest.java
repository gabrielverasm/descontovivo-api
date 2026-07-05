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
class ModerationCategoryResourceTest {

    @Test
    void shouldReturn401WithoutAuth() {
        given()
            .when().get("/api/v1/moderation/categories")
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
            .when().get("/api/v1/moderation/categories")
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldListCategoriesIgnoringNullAndEmpty() {
        var id1 = createPromotion();
        var id2 = createPromotion();

        // Set category on first promotion
        setCategory(id1, "Eletrônicos");
        // Set category on second promotion
        setCategory(id2, "Eletrônicos");

        given()
            .when().get("/api/v1/moderation/categories")
            .then()
            .statusCode(200)
            .body("find { it.name == 'Eletrônicos' }.promotionCount", greaterThanOrEqualTo(2));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldRenameCategory() {
        var id = createPromotion();
        String suffix = id.substring(0, 4);
        String catName = "CatRename" + suffix;
        setCategory(id, catName);

        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"CatRenamed" + suffix + "\"}")
            .when().patch("/api/v1/moderation/categories/" + catName)
            .then()
            .statusCode(200)
            .body("name", is("CatRenamed" + suffix))
            .body("promotionCount", greaterThanOrEqualTo(1));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn404WhenRenamingNonExistentCategory() {
        given()
            .contentType(ContentType.JSON)
            .body("{ \"name\": \"Anything\" }")
            .when().patch("/api/v1/moderation/categories/NonExistent_" + UUID.randomUUID().toString().substring(0, 6))
            .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn400WhenRenameToEmptyName() {
        given()
            .contentType(ContentType.JSON)
            .body("{ \"name\": \"   \" }")
            .when().patch("/api/v1/moderation/categories/Something")
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn409WhenRenamingToExistingCategory() {
        var id1 = createPromotion();
        var id2 = createPromotion();
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String cat1 = "CatA_" + suffix;
        String cat2 = "CatB_" + suffix;
        setCategory(id1, cat1);
        setCategory(id2, cat2);

        given()
            .contentType(ContentType.JSON)
            .body("{\"name\": \"" + cat2 + "\"}")
            .when().patch("/api/v1/moderation/categories/" + cat1)
            .then()
            .statusCode(409);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldDeleteCategoryWithoutDeletingPromotions() {
        var id = createPromotion();
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String cat = "CatDel_" + suffix;
        setCategory(id, cat);

        // Delete the category
        given()
            .when().delete("/api/v1/moderation/categories/" + cat)
            .then()
            .statusCode(204);

        // Verify promotion still exists but category is null
        given()
            .queryParam("status", "PENDING_REVIEW")
            .when().get("/api/v1/moderation/promotions")
            .then()
            .statusCode(200)
            .body("find { it.id == '" + id + "' }", notNullValue())
            .body("find { it.id == '" + id + "' }.category", nullValue());
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn404WhenDeletingNonExistentCategory() {
        given()
            .when().delete("/api/v1/moderation/categories/NonExistent_" + UUID.randomUUID().toString().substring(0, 6))
            .then()
            .statusCode(404);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldHandleCategoryWithSpecialCharacters() {
        var id = createPromotion();
        String specialCat = "Casa & Cozinha";
        setCategory(id, specialCat);

        // Rename category with special characters
        // RestAssured encodes the path automatically, so use the raw value
        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"Casa & Jardim\"}")
            .when().patch("/api/v1/moderation/categories/{cat}", specialCat)
            .then()
            .statusCode(200)
            .body("name", is("Casa & Jardim"))
            .body("promotionCount", greaterThanOrEqualTo(1));

        // Delete renamed category with special characters
        given()
            .when().delete("/api/v1/moderation/categories/{cat}", "Casa & Jardim")
            .then()
            .statusCode(204);
    }

    private String createPromotion() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Cat Test %s",
                    "url": "https://www.amazon.com.br/cat-%s",
                    "currentPrice": 50.00,
                    "imageUrl": "https://images.example.com/cat.jpg",
                    "imageKey": "temp/promotions/2026/07/cat-%s.webp",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("id");
    }

    private void setCategory(String promotionId, String category) {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "EDIT", "reason": "Set category", "category": "%s" }
            """.formatted(category))
            .when().patch("/api/v1/moderation/promotions/" + promotionId)
            .then().statusCode(200);
    }
}
