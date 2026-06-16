package br.com.descontovivo.promotion.api;

import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;

@QuarkusTest
class PromotionResourceTest {

    private static final String ADMIN_TOKEN = "test-admin-token";

    @Test
    void shouldCreatePromotionAsPendingReview() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Echo Dot 5a geração",
                    "url": "https://www.amazon.com.br/echo-dot-5",
                    "description": "Echo Dot 5a geração com preço incrível",
                    "currentPrice": 199.00,
                    "originalPrice": 399.00,
                    "imageUrl": "https://images.example.com/echo-dot.jpg",
                    "storeSlug": "amazon"
                }
            """)
            .when().post("/api/v1/promotions")
            .then()
            .statusCode(201)
            .body("status", is("PENDING_REVIEW"))
            .body("slug", notNullValue())
            .body("id", notNullValue());
    }

    @Test
    void shouldNotListPendingPromotions() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Produto Invisível",
                    "url": "https://www.amazon.com.br/invisivel",
                    "description": "Este produto não deve aparecer no feed",
                    "currentPrice": 50.00,
                    "imageUrl": "https://images.example.com/inv.jpg",
                    "storeSlug": "amazon"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(201);

        given()
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.find { it.title == 'Produto Invisível' }", nullValue());
    }

    @Test
    void shouldApproveAndListPublished() {
        String id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Kindle Paperwhite Oferta",
                    "url": "https://www.amazon.com.br/kindle-pw",
                    "description": "Kindle Paperwhite com desconto",
                    "currentPrice": 449.00,
                    "originalPrice": 699.00,
                    "imageUrl": "https://images.example.com/kindle.jpg",
                    "storeSlug": "amazon"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", ADMIN_TOKEN)
            .body("""
                { "action": "APPROVE", "reason": "Oferta válida" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("PUBLISHED"))
            .body("publishedAt", notNullValue());

        given()
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.find { it.id == '" + id + "' }.title", is("Kindle Paperwhite Oferta"));
    }

    @Test
    void shouldGetPublishedBySlug() {
        String slug = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Fire TV Stick Promo",
                    "url": "https://www.amazon.com.br/fire-tv",
                    "description": "Fire TV Stick com desconto imperdível",
                    "currentPrice": 199.00,
                    "imageUrl": "https://images.example.com/firetv.jpg",
                    "storeSlug": "amazon"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("slug");

        String id = given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Fire TV Stick Promo",
                    "url": "https://www.amazon.com.br/fire-tv",
                    "description": "Fire TV Stick com desconto imperdível",
                    "currentPrice": 199.00,
                    "imageUrl": "https://images.example.com/firetv.jpg",
                    "storeSlug": "amazon"
                }
            """)
            .when().post("/api/v1/promotions")
            .then()
            .extract().jsonPath().getString("id");

        // Approve the first one by slug lookup in moderation
        String firstId = given()
            .header("X-Admin-Token", ADMIN_TOKEN)
            .when().get("/api/v1/moderation/promotions?status=PENDING_REVIEW")
            .then().statusCode(200)
            .extract().jsonPath().getString("find { it.slug == '" + slug + "' }.id");

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Token", ADMIN_TOKEN)
            .body("""
                { "action": "APPROVE", "reason": "OK" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + firstId)
            .then().statusCode(200);

        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(200)
            .body("slug", is(slug))
            .body("title", is("Fire TV Stick Promo"));
    }

    @Test
    void shouldReturn409OnDuplicateSameDay() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Produto Duplicado",
                    "url": "https://www.magalu.com.br/dup-item",
                    "description": "Descrição do produto duplicado",
                    "currentPrice": 100.00,
                    "imageUrl": "https://images.example.com/dup.jpg",
                    "storeSlug": "magalu"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(201);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Produto Duplicado Outra",
                    "url": "https://www.magalu.com.br/dup-item?utm_source=fb",
                    "description": "Descrição do produto duplicado",
                    "currentPrice": 90.00,
                    "imageUrl": "https://images.example.com/dup2.jpg",
                    "storeSlug": "magalu"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(409);
    }

    @Test
    void shouldFailWithNonExistentStore() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Promoção Fantasma",
                    "url": "https://www.loja-fake.com.br/item",
                    "description": "Loja não existe",
                    "currentPrice": 10.00,
                    "imageUrl": "https://images.example.com/fake.jpg",
                    "storeSlug": "loja-fantasma"
                }
            """)
            .when().post("/api/v1/promotions")
            .then().statusCode(404);
    }

    @Test
    void shouldReturn403WithoutAdminToken() {
        given()
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(403);
    }

    @Test
    void shouldReturn403WithWrongToken() {
        given()
            .header("X-Admin-Token", "wrong-token")
            .when().get("/api/v1/moderation/promotions")
            .then().statusCode(403);
    }

    @Test
    void shouldListSeedPromotionsInFeed() {
        given()
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.size()", greaterThanOrEqualTo(12));
    }
}
