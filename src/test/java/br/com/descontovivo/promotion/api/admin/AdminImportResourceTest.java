package br.com.descontovivo.promotion.api.admin;

import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.upload.mock.MockRemoteImageImportService;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(AdminImportResourceTest.Profile.class)
class AdminImportResourceTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "admin.import.token", "test-secret-token-123",
                "admin.import.default-author", "gabrielveras"
            );
        }
    }

    private static final String IMPORT_PATH = "/api/v1/admin/promotions/import";

    @Inject
    MockRemoteImageImportService mockImageImport;

    @BeforeEach
    void setUp() {
        mockImageImport.reset();
    }

    // --- Security tests ---

    @Test
    void shouldReturn403WithoutAuthOrToken() {
        given()
            .contentType(ContentType.JSON)
            .body(validImportBody("no-auth-" + uid()))
            .when().post(IMPORT_PATH)
            .then().statusCode(403);
    }

    @Test
    void shouldReturn403WithInvalidToken() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "wrong-token")
            .body(validImportBody("invalid-token-" + uid()))
            .when().post(IMPORT_PATH)
            .then().statusCode(403);
    }

    @Test
    void shouldAllowAccessWithValidToken() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody("token-access-" + uid()))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("dryRun", is(false));
    }

    @Test
    @TestSecurity(user = "admin-user", roles = {"admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "admin-user")
    })
    void shouldAllowAccessWithAdminRole() {
        given()
            .contentType(ContentType.JSON)
            .body(validImportBody("admin-role-" + uid()))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));
    }

    @Test
    @TestSecurity(user = "regular-user", roles = {"user"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "user-sub"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "regular-user")
    })
    void shouldReturn403ForNonAdminRole() {
        given()
            .contentType(ContentType.JSON)
            .body(validImportBody("non-admin-" + uid()))
            .when().post(IMPORT_PATH)
            .then().statusCode(403);
    }

    // --- Dry run tests ---

    @Test
    void shouldNotPersistOnDryRun() {
        var sourceId = "dryrun-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true))
            .body("created", is(1))
            .body("skipped", is(0));

        // Real import should still create (proves dryRun did not persist)
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("skipped", is(0));
    }

    @Test
    void shouldNotUploadToR2OnDryRun() {
        var sourceId = "dryrun-noupload-" + uid();
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true))
            .body("created", is(1));

        // Mock should NOT have received any importImage calls
        assertTrue(mockImageImport.getImportedUrls().isEmpty(),
                "DryRun should not trigger image import to R2");
    }

    // --- Persistence and dedup tests ---

    @Test
    void shouldPersistOnRealImport() {
        var sourceId = "persist-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(false))
            .body("created", is(1));

        // Second import same sourceId should be skipped
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(0))
            .body("skipped", is(1));
    }

    @Test
    void shouldSkipDuplicateSourceIdWithinSameBatch() {
        var sourceId = "intra-dup-" + uid();
        var body = """
            {
              "items": [
                {
                  "sourceId": "%s",
                  "title": "First item",
                  "description": "First",
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/first-%s",
                  "imageUrl": "https://images.example.com/a.jpg",
                  "currentPrice": 100.00
                },
                {
                  "sourceId": "%s",
                  "title": "Duplicate sourceId",
                  "description": "Should be skipped",
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/second-%s",
                  "imageUrl": "https://images.example.com/b.jpg",
                  "currentPrice": 200.00
                }
              ]
            }
        """.formatted(sourceId, sourceId, sourceId, sourceId);

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(body)
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("skipped", is(1));
    }

    // --- Image import to R2 tests ---

    @Test
    void shouldImportImageToR2OnRealImport() {
        var sourceId = "img-r2-" + uid();
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        // Should have called importImage with the external URL
        assertEquals(1, mockImageImport.getImportedUrls().size());
        assertTrue(mockImageImport.getImportedUrls().get(0).contains("images.example.com"));
    }

    @Test
    void shouldSavePromotionWithR2ImageUrl() {
        var sourceId = "r2-url-" + uid();
        var title = "R2 Image Test " + sourceId;

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "description": "Test R2 image",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/r2-url-%s",
                    "imageUrl": "https://images.example.com/external.jpg",
                    "currentPrice": 149.90
                  }]
                }
            """.formatted(sourceId, title, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        // Verify the promotion was saved with R2 URL (not external URL) and WebP format
        var slug = SlugGenerator.fromTitle(title);
        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(200)
            .body("imageUrl", startsWith("https://img.descontovivo.com.br/promotions/imported/"))
            .body("imageUrl", endsWith(".webp"))
            .body("imageUrl", not(containsString("images.example.com")));
    }

    @Test
    void shouldNotSaveExternalImageUrlInPromotion() {
        var sourceId = "no-ext-" + uid();
        var title = "No External Image " + sourceId;

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "description": "Must not save external URL",
                    "marketplace": "MAGALU",
                    "storeName": "Magazine Luiza",
                    "productUrl": "https://example.com/no-ext-%s",
                    "imageUrl": "https://m.media-amazon.com/images/I/external.jpg",
                    "currentPrice": 299.90
                  }]
                }
            """.formatted(sourceId, title, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        var slug = SlugGenerator.fromTitle(title);
        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(200)
            .body("imageUrl", not(containsString("m.media-amazon.com")))
            .body("imageUrl", startsWith("https://img.descontovivo.com.br/"));
    }

    @Test
    void shouldFailItemWhenImageImportFails() {
        var sourceId = "img-fail-" + uid();
        mockImageImport.setShouldFail(true);
        mockImageImport.setFailureMessage("Erro ao baixar imagem: conexão falhou");

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(0))
            .body("errors.size()", is(1))
            .body("errors[0].sourceId", is(sourceId))
            .body("errors[0].field", is("imageUrl"))
            .body("errors[0].message", containsString("Erro ao baixar imagem"));
    }

    @Test
    void shouldRejectLocalhostUrlOnDryRun() {
        var sourceId = "ssrf-dry-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "SSRF test",
                    "description": "Should block localhost",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/ssrf-%s",
                    "imageUrl": "http://localhost:8080/internal-image.jpg",
                    "currentPrice": 99.00
                  }]
                }
            """.formatted(sourceId, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(0))
            .body("errors.size()", is(1))
            .body("errors[0].field", is("imageUrl"))
            .body("errors[0].message", containsString("bloqueado"));
    }

    @Test
    void shouldRejectPrivateIpUrlOnDryRun() {
        var sourceId = "ssrf-ip-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "SSRF IP test",
                    "description": "Should block private IP",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/ssrf-ip-%s",
                    "imageUrl": "http://192.168.1.1/image.jpg",
                    "currentPrice": 99.00
                  }]
                }
            """.formatted(sourceId, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(0))
            .body("errors.size()", is(1))
            .body("errors[0].field", is("imageUrl"))
            .body("errors[0].message", containsString("bloqueado"));
    }

    @Test
    void shouldNotPersistPromotionWhenImageFails() {
        var sourceId = "no-persist-" + uid();
        var title = "No Persist On Fail " + sourceId;
        mockImageImport.setShouldFail(true);
        mockImageImport.setFailureMessage("Simulated R2 failure");

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "description": "Must not be saved",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/nopersist-%s",
                    "imageUrl": "https://images.example.com/fail.jpg",
                    "currentPrice": 199.90
                  }]
                }
            """.formatted(sourceId, title, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(0))
            .body("errors.size()", is(1));

        // Promotion should NOT exist
        var slug = SlugGenerator.fromTitle(title);
        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(404);
    }

    // --- publishAt tests ---

    @Test
    void shouldAssignPublishAtFromImportStartedAtWhenNotProvided() {
        var sourceId = "no-publishat-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(importBodyWithoutPublishAt(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        // Should appear in public listing immediately (publishAt = importStartedAt <= now)
        given()
            .queryParam("q", "No PublishAt " + sourceId)
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.size()", greaterThanOrEqualTo(1));
    }

    @Test
    void shouldNotShowFuturePromotionInPublicListing() {
        var sourceId = "future-list-" + uid();
        var futureDate = OffsetDateTime.now().plusDays(30).toString();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(importBodyWithPublishAt(sourceId, futureDate))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        given()
            .queryParam("q", "Future Promo " + sourceId)
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content", empty());
    }

    @Test
    void shouldNotShowFuturePromotionInDetailBySlug() {
        var sourceId = "future-slug-" + uid();
        var title = "Future Detail " + sourceId;
        var futureDate = OffsetDateTime.now().plusDays(30).toString();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "batchId": "test-future-detail",
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "description": "Future item for slug test",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/future-slug-%s",
                    "imageUrl": "https://images.example.com/future.jpg",
                    "currentPrice": 199.00,
                    "publishAt": "%s"
                  }]
                }
            """.formatted(sourceId, title, sourceId, futureDate))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        // Use the same slug logic as the service
        var expectedSlug = SlugGenerator.fromTitle(title);

        given()
            .when().get("/api/v1/promotions/" + expectedSlug)
            .then()
            .statusCode(404);
    }

    @Test
    void shouldShowPastPublishAtInListing() {
        var sourceId = "past-" + uid();
        var pastDate = OffsetDateTime.now().minusHours(1).toString();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(importBodyWithPublishAt(sourceId, pastDate))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));

        given()
            .queryParam("q", "Future Promo " + sourceId)
            .when().get("/api/v1/promotions")
            .then()
            .statusCode(200)
            .body("content.size()", greaterThanOrEqualTo(1));
    }

    // --- Validation tests ---

    @Test
    void shouldReturnValidationErrors() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "err-item",
                    "title": "",
                    "description": "",
                    "marketplace": "",
                    "storeName": "",
                    "productUrl": "",
                    "imageUrl": "",
                    "currentPrice": 0
                  }]
                }
            """)
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("errors.size()", greaterThan(0))
            .body("created", is(0));
    }

    @Test
    void shouldRejectOriginalPriceLessThanCurrentPrice() {
        var id = uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "price-err-%s",
                    "title": "Price Error Test",
                    "description": "Original less than current",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/price-err-%s",
                    "imageUrl": "https://images.example.com/p.jpg",
                    "currentPrice": 500.00,
                    "originalPrice": 100.00
                  }]
                }
            """.formatted(id, id))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("errors.size()", is(1))
            .body("errors[0].field", is("originalPrice"));
    }

    // --- Slug tests ---

    @Test
    void shouldHandleDuplicateSlugGracefully() {
        var id1 = "slug-dup-1-" + uid();
        var id2 = "slug-dup-2-" + uid();
        var body = """
            {
              "items": [
                {
                  "sourceId": "%s",
                  "title": "Same Title Item",
                  "description": "First item",
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/%s",
                  "imageUrl": "https://images.example.com/a.jpg",
                  "currentPrice": 100.00
                },
                {
                  "sourceId": "%s",
                  "title": "Same Title Item",
                  "description": "Second item",
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/%s",
                  "imageUrl": "https://images.example.com/b.jpg",
                  "currentPrice": 200.00
                }
              ]
            }
        """.formatted(id1, id1, id2, id2);

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(body)
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(2))
            .body("errors", empty());
    }

    // --- Batch ID tests ---

    @Test
    void shouldGenerateBatchIdWhenNotProvided() {
        var id = uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "auto-batch-%s",
                    "title": "Auto Batch",
                    "description": "Test auto batch",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/auto-batch-%s",
                    "imageUrl": "https://images.example.com/x.jpg",
                    "currentPrice": 50.00
                  }]
                }
            """.formatted(id, id))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("batchId", startsWith("import-"));
    }

    // --- Helpers ---

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private String validImportBody(String sourceId) {
        return """
            {
              "batchId": "test-batch",
              "items": [{
                "sourceId": "%s",
                "title": "Test Import %s",
                "description": "A valid import item",
                "marketplace": "AMAZON",
                "storeName": "Amazon",
                "productUrl": "https://example.com/%s",
                "imageUrl": "https://images.example.com/test.jpg",
                "currentPrice": 199.90
              }]
            }
        """.formatted(sourceId, sourceId, sourceId);
    }

    private String importBodyWithoutPublishAt(String sourceId) {
        return """
            {
              "batchId": "test-no-publishat",
              "items": [{
                "sourceId": "%s",
                "title": "No PublishAt %s",
                "description": "Item without publishAt",
                "marketplace": "AMAZON",
                "storeName": "Amazon",
                "productUrl": "https://example.com/nopub-%s",
                "imageUrl": "https://images.example.com/nopub.jpg",
                "currentPrice": 99.00
              }]
            }
        """.formatted(sourceId, sourceId, sourceId);
    }

    private String importBodyWithPublishAt(String sourceId, String publishAt) {
        return """
            {
              "batchId": "test-publishat",
              "items": [{
                "sourceId": "%s",
                "title": "Future Promo %s",
                "description": "Item with custom publishAt",
                "marketplace": "AMAZON",
                "storeName": "Amazon",
                "productUrl": "https://example.com/pub-%s",
                "imageUrl": "https://images.example.com/pub.jpg",
                "currentPrice": 299.00,
                "publishAt": "%s"
              }]
            }
        """.formatted(sourceId, sourceId, sourceId, publishAt);
    }
}
