package br.com.descontovivo.moderation.api;

import io.agroal.api.AgroalDataSource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import jakarta.inject.Inject;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

@QuarkusTest
class ModerationCategoryResourceTest {

    @Inject
    AgroalDataSource dataSource;

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
    void shouldReturn200WithEmptyListWhenNoCategories() {
        // Without setting any category, endpoint should return 200 with empty or non-null list
        given()
            .when().get("/api/v1/moderation/categories")
            .then()
            .statusCode(200)
            .body("$", notNullValue());
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
        var secondId = createPromotion();
        String suffix = id.substring(0, 4);
        String catName = "CatRename" + suffix;
        String renamed = "CatRenamed" + suffix;
        setCategory(id, catName);
        setCategory(secondId, catName);

        given()
            .contentType(ContentType.JSON)
            .body("{\"name\":\"" + renamed + "\"}")
            .when().patch("/api/v1/moderation/categories/" + catName)
            .then()
            .statusCode(200)
            .body("name", is(renamed))
            .body("promotionCount", greaterThanOrEqualTo(2));

        given()
            .queryParam("status", "PENDING_REVIEW")
            .when().get("/api/v1/moderation/promotions")
            .then()
            .statusCode(200)
            .body("find { it.id == '%s' }.category".formatted(id), is(renamed))
            .body("find { it.id == '%s' }.categories".formatted(id), contains(renamed))
            .body("find { it.id == '%s' }.category".formatted(secondId), is(renamed))
            .body("find { it.id == '%s' }.categories".formatted(secondId), contains(renamed));
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
    void shouldCompactPositionsAndSynchronizeLegacyCategoryAfterGlobalDeletes() throws SQLException {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String casa = "Casa_" + suffix;
        String ofertas = "Ofertas_" + suffix;
        String games = "Games_" + suffix;
        var firstId = createPromotion();
        var offersOnlyId = createPromotion();
        var secondId = createPromotion();

        setCategory(firstId, casa);
        setCategory(offersOnlyId, ofertas);
        setCategory(secondId, games);
        setCategories(firstId, List.of(casa, ofertas, games));
        setCategories(secondId, List.of(games, ofertas, casa));

        assertPersistedCategories(firstId, List.of(casa, ofertas, games));
        assertPersistedCategories(secondId, List.of(games, ofertas, casa));

        deleteCategory(ofertas);

        assertPromotionCategories(firstId, casa, List.of(casa, games));
        assertPromotionCategories(offersOnlyId, null, List.of());
        assertPromotionCategories(secondId, games, List.of(games, casa));
        assertPersistedCategories(firstId, List.of(casa, games));
        assertPersistedCategories(secondId, List.of(games, casa));

        deleteCategory(casa);

        assertPromotionCategories(firstId, games, List.of(games));
        assertPromotionCategories(secondId, games, List.of(games));
        assertPersistedCategories(firstId, List.of(games));
        assertPersistedCategories(secondId, List.of(games));

        deleteCategory(games);

        assertPromotionCategories(firstId, null, List.of());
        assertPromotionCategories(secondId, null, List.of());
        assertPersistedCategories(firstId, List.of());
        assertPersistedCategories(secondId, List.of());
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldPersistAndReloadCategoryReorderingWithoutConstraintViolations() throws SQLException {
        String suffix = UUID.randomUUID().toString().substring(0, 6);
        String casa = "CasaOrder_" + suffix;
        String ofertas = "OfertasOrder_" + suffix;
        var firstId = createPromotion();
        var secondId = createPromotion();

        setCategory(firstId, casa);
        setCategory(secondId, ofertas);
        setCategories(firstId, List.of(casa, ofertas));
        assertPersistedCategories(firstId, List.of(casa, ofertas));

        setCategories(firstId, List.of(ofertas, casa));

        assertPromotionCategories(firstId, ofertas, List.of(ofertas, casa));
        assertPersistedCategories(firstId, List.of(ofertas, casa));
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

    private void setCategories(String promotionId, List<String> categories) {
        String jsonCategories = categories.stream()
                .map(category -> "\"" + category + "\"")
                .collect(java.util.stream.Collectors.joining(","));
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Set categories",
                    "categories": [%s]
                }
            """.formatted(jsonCategories))
            .when().patch("/api/v1/moderation/promotions/" + promotionId)
            .then().statusCode(200);
    }

    private void deleteCategory(String category) {
        given()
            .when().delete("/api/v1/moderation/categories/{category}", category)
            .then().statusCode(204);
    }

    private void assertPromotionCategories(String promotionId, String legacyCategory, List<String> expected) {
        var json = given()
            .queryParam("status", "PENDING_REVIEW")
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(200)
            .extract().jsonPath();
        assertEquals(legacyCategory, json.getString("find { it.id == '%s' }.category".formatted(promotionId)));
        List<String> categories = json.getList(
                "find { it.id == '%s' }.categories".formatted(promotionId),
                String.class);
        assertEquals(expected, categories);
        assertFalse(categories.stream().anyMatch(java.util.Objects::isNull));
    }

    private void assertPersistedCategories(String promotionId, List<String> expected) throws SQLException {
        var categories = new ArrayList<String>();
        var positions = new ArrayList<Integer>();
        try (var connection = dataSource.getConnection();
             var statement = connection.prepareStatement("""
                     SELECT category, position
                     FROM promotion_category
                     WHERE promotion_id = CAST(? AS UUID)
                     ORDER BY position
                     """)) {
            statement.setString(1, promotionId);
            try (var result = statement.executeQuery()) {
                while (result.next()) {
                    categories.add(result.getString("category"));
                    positions.add(result.getInt("position"));
                }
            }
        }
        assertEquals(expected, categories);
        assertEquals(
                java.util.stream.IntStream.range(0, expected.size()).boxed().toList(),
                positions);
        assertFalse(categories.stream().anyMatch(java.util.Objects::isNull));
    }
}
