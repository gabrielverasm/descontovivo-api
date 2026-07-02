package br.com.descontovivo.moderation.api;

import br.com.descontovivo.upload.mock.MockR2StorageService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
class ModerationResourceTest {

    @Inject
    MockR2StorageService mockR2;

    @BeforeEach
    void setUp() {
        mockR2.clearDeletedKeys();
        mockR2.setShouldFailOnDelete(false);
        mockR2.setShouldFailOnPromote(false);
    }

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
            .body("publishedAt", notNullValue())
            .body("authorUsername", is("admin-user"));
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

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldDeleteR2ImageOnReject() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REJECT", "reason": "Spam" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("REJECTED"));

        assertFalse(mockR2.getDeletedKeys().isEmpty(), "Should have deleted image from R2");
        assertTrue(mockR2.getDeletedKeys().get(0).startsWith("promotions/"));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"user", "moderator", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "admin-user")
    })
    void shouldDeleteR2ImageOnRemove() {
        var id = createPromotion();

        // First approve
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "Valid" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        mockR2.clearDeletedKeys();

        // Then remove
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "Expired offer" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("REMOVED"));

        assertFalse(mockR2.getDeletedKeys().isEmpty(), "Should have deleted image from R2");
        assertTrue(mockR2.getDeletedKeys().get(0).startsWith("promotions/"));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturn403WhenModeratorTriesToRemove() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REMOVE", "reason": "Tentativa de remover como moderator" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(403);

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "R2 delete should NOT be called on 403");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotDeleteR2ImageOnApprove() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "Good deal" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Should NOT delete image on APPROVE");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotDeleteR2ImageOnEdit() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "EDIT", "reason": "Fix title", "title": "Fixed Title" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Should NOT delete image on EDIT without image change");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldEditImageWithValidTempKey() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Trocar imagem",
                    "imageKey": "temp/promotions/2026/07/new-image.webp"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("imageUrl", is("https://img.descontovivo.com.br/promotions/2026/07/new-image.webp"));

        // Old image should have been deleted
        assertFalse(mockR2.getDeletedKeys().isEmpty(), "Old image should be deleted after replacement");
        assertTrue(mockR2.getDeletedKeys().stream()
                .anyMatch(k -> k.startsWith("promotions/2026/06/mod-")));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldRejectInvalidImageKey() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Trocar imagem",
                    "imageKey": "promotions/2026/07/direct-injection.webp"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(422);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldRejectExternalImageUrlWithoutImageKey() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Injetar URL",
                    "imageUrl": "https://evil.com/malware.jpg"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(422);
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldKeepCurrentImageWhenNoImageFieldsSent() {
        var id = createPromotion();

        var originalImageUrl = given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "OK" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200)
            .extract().jsonPath().getString("imageUrl");

        mockR2.clearDeletedKeys();

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "EDIT", "reason": "Fix title only", "title": "New Title" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("imageUrl", is(originalImageUrl))
            .body("title", is("New Title"));

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Should NOT touch image when not changing it");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotDeleteOldImageWhenPromoteFails() {
        var id = createPromotion();
        mockR2.setShouldFailOnPromote(true);

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Trocar imagem",
                    "imageKey": "temp/promotions/2026/07/fail.webp"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(500);

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Old image must NOT be deleted when promote fails");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotFailWhenImageKeyIsNullOrBlank() {
        // Directly verify the service handles null/blank without error
        mockR2.deletePromotionImageIfPresent(null);
        mockR2.deletePromotionImageIfPresent("");
        mockR2.deletePromotionImageIfPresent("   ");
        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Should not attempt delete with null/blank imageKey");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotBlockRejectionWhenR2DeleteFails() {
        var id = createPromotion();
        mockR2.setShouldFailOnDelete(true);

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "REJECT", "reason": "Spam" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("status", is("REJECTED"));

        assertTrue(mockR2.getDeletedKeys().isEmpty(), "Delete should have been attempted but failed silently");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldEditCurationFieldsViaPatch() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Ajuste de curadoria",
                    "soldBy": "Loja XPTO",
                    "deliveredBy": "Amazon",
                    "category": "Eletrônicos",
                    "availability": "AVAILABLE"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("soldBy", is("Loja XPTO"))
            .body("deliveredBy", is("Amazon"))
            .body("category", is("Eletrônicos"))
            .body("availability", is("AVAILABLE"));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldReturnCurationFieldsOnGetModeration() {
        var id = createPromotion();

        // Set curation fields
        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Curadoria",
                    "soldBy": "Seller ABC",
                    "deliveredBy": "Correios",
                    "category": "Casa"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then().statusCode(200);

        // Verify GET returns the fields
        given()
            .queryParam("status", "PENDING_REVIEW")
            .when().get("/api/v1/moderation/promotions")
            .then()
            .statusCode(200)
            .body("find { it.id == '%s' }.soldBy".formatted(id), is("Seller ABC"))
            .body("find { it.id == '%s' }.deliveredBy".formatted(id), is("Correios"))
            .body("find { it.id == '%s' }.category".formatted(id), is("Casa"))
            .body("find { it.id == '%s' }.url".formatted(id), notNullValue());
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldNotExposeInternalFieldsInModerationResponse() {
        var id = createPromotion();

        var body = given()
            .contentType(ContentType.JSON)
            .body("""
                { "action": "APPROVE", "reason": "OK" }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .extract().body().asString();

        assertFalse(body.contains("marketplace"), "Should not expose marketplace");
        assertFalse(body.contains("sellerName"), "Should not expose sellerName");
        assertFalse(body.contains("imageKey"), "Should not expose imageKey");
        assertFalse(body.contains("normalizedUrl"), "Should not expose normalizedUrl");
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldEditStoreViaStoreNameInModeration() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Corrigir loja",
                    "storeName": "Pague Menos"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("store.name", is("Pague Menos"))
            .body("store.slug", is("pague-menos"));
    }

    @Test
    @TestSecurity(user = "mod-user", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "mod-user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "mod-user")
    })
    void shouldPreferStoreNameOverStoreSlugInModeration() {
        var id = createPromotion();

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "action": "EDIT",
                    "reason": "Corrigir loja por nome",
                    "storeName": "Fast Shop",
                    "storeSlug": "amazon"
                }
            """)
            .when().patch("/api/v1/moderation/promotions/" + id)
            .then()
            .statusCode(200)
            .body("store.name", is("Fast Shop"))
            .body("store.slug", is("fast-shop"));
    }

    private String createPromotion() {
        var uid = UUID.randomUUID().toString().substring(0, 8);
        return given()
            .contentType(ContentType.JSON)
            .body("""
                {
                    "title": "Mod Test %s",
                    "url": "https://www.amazon.com.br/mod-%s",
                    "currentPrice": 100.00,
                    "imageUrl": "https://images.example.com/mod.jpg",
                    "imageKey": "temp/promotions/2026/06/mod-%s.webp",
                    "storeSlug": "amazon"
                }
            """.formatted(uid, uid, uid))
            .when().post("/api/v1/promotions")
            .then().statusCode(201)
            .extract().jsonPath().getString("id");
    }
}
