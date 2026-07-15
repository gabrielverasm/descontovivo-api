package br.com.descontovivo.promotion.api.admin;

import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.promotion.support.SlugGenerator;
import br.com.descontovivo.upload.mock.MockR2StorageService;
import br.com.descontovivo.upload.mock.MockRemoteImageImportService;
import io.quarkus.narayana.jta.QuarkusTransaction;
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

    @Inject
    MockR2StorageService mockR2Storage;

    @Inject
    PromotionRepository promotionRepository;

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
    void shouldAllowEquivalentPromotionPublishedFiveDaysAgo() {
        var sourceId = "five-days-" + uid();
        importSuccessfully(sourceId);
        setPublishedAt(sourceId, OffsetDateTime.now().minusDays(5));

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
    void shouldAllowEquivalentPromotionWithoutPublishedAt() {
        var sourceId = "no-date-" + uid();
        importSuccessfully(sourceId);
        setPublishedAt(sourceId, null);

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
    void shouldAllowEquivalentPromotionWithFuturePublishedAt() {
        var sourceId = "future-date-" + uid();
        importSuccessfully(sourceId);
        setPublishedAt(sourceId, OffsetDateTime.now().plusDays(1));

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
    void shouldSkipDuplicateSourceIdWithinSameBatch() {
        var sourceId = "intra-dup-" + uid();
        var body = """
            {
              "items": [
                {
                  "sourceId": "%s",
                  "title": "First item",
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/first-%s",
                  "imageUrl": "https://images.example.com/a.jpg",
                  "currentPrice": 100.00
                },
                {
                  "sourceId": "%s",
                  "title": "Duplicate sourceId",
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
                  "marketplace": "AMAZON",
                  "storeName": "Amazon",
                  "productUrl": "https://example.com/%s",
                  "imageUrl": "https://images.example.com/a.jpg",
                  "currentPrice": 100.00
                },
                {
                  "sourceId": "%s",
                  "title": "Same Title Item",
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

    // --- authorUsername tests ---

    @Test
    @TestSecurity(user = "some-principal-id", roles = {"admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "some-principal-id"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "admin-user")
    })
    void shouldSetAuthorUsernameFromAuthenticatedAdmin() {
        var sourceId = "author-admin-" + uid();
        var title = "Author Admin Test " + sourceId;

        given()
            .contentType(ContentType.JSON)
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/author-admin-%s",
                    "imageUrl": "https://images.example.com/author.jpg",
                    "currentPrice": 99.90
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
            // deve ser preferred_username, não o principal name "some-principal-id"
            .body("authorUsername", is("admin-user"))
            .body("authorUsername", not("some-principal-id"));
    }

    @Test
    void shouldSetAuthorUsernameToDefaultWhenAccessedViaToken() {
        var sourceId = "author-token-" + uid();
        var title = "Author Token Test " + sourceId;

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/author-token-%s",
                    "imageUrl": "https://images.example.com/token.jpg",
                    "currentPrice": 149.90
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
            .body("authorUsername", is("gabrielveras"));
    }

    // --- Serialization / DryRun with real-world payload tests ---

    @Test
    void shouldReturnFullJsonResponseOnDryRunWithAmazonPayload() {
        var sourceId = "amz-dry-" + uid();
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "Echo Dot 5ª Geração com Alexa",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "sellerName": null,
                    "soldBy": null,
                    "deliveredBy": null,
                    "productUrl": "https://link.amazon/B0aAd8l9g",
                    "imageUrl": "https://m.media-amazon.com/images/I/71-hdigva3L._AC_SY300_SX300_QL70_ML2_.jpg",
                    "currentPrice": 284.05,
                    "originalPrice": 399.00,
                    "publishAt": "2026-07-02T08:30:00-03:00"
                  }]
                }
            """.formatted(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true))
            .body("created", is(1))
            .body("skipped", is(0))
            .body("errors", empty())
            .body("batchId", notNullValue());
    }

    @Test
    void shouldReturnErrorsAsJsonOnDryRunWithInvalidItem() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body("""
                {
                  "items": [{
                    "sourceId": "invalid-item",
                    "title": "",
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
            .body("dryRun", is(true))
            .body("created", is(0))
            .body("errors.size()", greaterThan(0))
            .body("errors[0].sourceId", is("invalid-item"))
            .body("errors[0].field", notNullValue())
            .body("errors[0].message", notNullValue());
    }

    @Test
    void shouldSerializeAdminImportResponseFieldsCorrectly() {
        // This test verifies that ALL fields of AdminImportResponse are serialized
        // (not returned as empty {} due to missing reflection metadata in native image)
        var sourceId = "serial-check-" + uid();
        var response = given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .extract().jsonPath();

        // Verify all fields are present and non-null in the response
        assertNotNull(response.getString("batchId"), "batchId should not be null");
        assertNotNull(response.getBoolean("dryRun"), "dryRun should not be null");
        assertEquals(true, response.getBoolean("dryRun"));
        assertEquals(1, response.getInt("created"));
        assertEquals(0, response.getInt("skipped"));
        assertNotNull(response.getList("errors"), "errors list should not be null");
    }


    // --- imageKey bypass tests (pre-uploaded images) ---

    @Test
    void shouldBypassRemoteImportWhenImageKeyIsProvided() {
        var sourceId = "imgkey-bypass-" + uid();
        var title = "ImageKey Bypass Test " + sourceId;
        var imageKey = "temp/promotions/2026/07/" + UUID.randomUUID() + ".webp";
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "%s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/imgkey-%s",
                    "imageUrl": "https://img.descontovivo.com.br/%s",
                    "imageKey": "%s",
                    "currentPrice": 199.90
                  }]
                }
            """.formatted(sourceId, title, sourceId, imageKey, imageKey))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("errors", empty());

        // RemoteImageImportService should NOT have been called
        assertTrue(mockImageImport.getImportedUrls().isEmpty(),
                "Should not call remoteImageImport when imageKey is provided");

        // Promotion should be saved with final R2 URL (promoted from temp)
        var slug = SlugGenerator.fromTitle(title);
        given()
            .when().get("/api/v1/promotions/" + slug)
            .then()
            .statusCode(200)
            .body("imageUrl", startsWith("https://img.descontovivo.com.br/promotions/"))
            .body("imageUrl", endsWith(".webp"))
            .body("imageUrl", not(containsString("temp/")));
    }

    @Test
    void shouldFallbackToRemoteImportWhenImageKeyIsAbsent() {
        var sourceId = "imgkey-fallback-" + uid();
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "Fallback Test %s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/fallback-%s",
                    "imageUrl": "https://images.example.com/external.jpg",
                    "currentPrice": 149.90
                  }]
                }
            """.formatted(sourceId, sourceId, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("errors", empty());

        // RemoteImageImportService SHOULD have been called
        assertEquals(1, mockImageImport.getImportedUrls().size(),
                "Should call remoteImageImport when imageKey is absent");
        assertTrue(mockImageImport.getImportedUrls().get(0).contains("images.example.com"));
    }

    @Test
    void shouldFallbackToRemoteImportWhenImageKeyIsInvalid() {
        var sourceId = "imgkey-invalid-" + uid();
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "Invalid Key Test %s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/invalid-key-%s",
                    "imageUrl": "https://images.example.com/fallback.jpg",
                    "imageKey": "../../../etc/passwd",
                    "currentPrice": 99.90
                  }]
                }
            """.formatted(sourceId, sourceId, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("errors", empty());

        // Should have fallen back to remote import since imageKey was invalid
        assertEquals(1, mockImageImport.getImportedUrls().size(),
                "Invalid imageKey should fallback to remote import");
    }

    @Test
    void shouldRejectPathTraversalInImageKey() {
        var sourceId = "imgkey-traversal-" + uid();
        mockImageImport.clearImportedUrls();

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "Traversal Test %s",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/traversal-%s",
                    "imageUrl": "https://images.example.com/safe.jpg",
                    "imageKey": "temp/promotions/../../secrets/key.webp",
                    "currentPrice": 99.90
                  }]
                }
            """.formatted(sourceId, sourceId, sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1))
            .body("errors", empty());

        // Should fallback to remote import (path traversal rejected by regex)
        assertEquals(1, mockImageImport.getImportedUrls().size(),
                "Path traversal imageKey should fallback to remote import");
    }

    @Test
    void shouldSkipUrlValidationOnDryRunWhenImageKeyIsValid() {
        var sourceId = "imgkey-dryrun-" + uid();
        var imageKey = "temp/promotions/2026/07/" + UUID.randomUUID() + ".webp";

        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .queryParam("dryRun", true)
            .body("""
                {
                  "items": [{
                    "sourceId": "%s",
                    "title": "DryRun ImageKey Test",
                    "marketplace": "AMAZON",
                    "storeName": "Amazon",
                    "productUrl": "https://example.com/dryrun-key-%s",
                    "imageUrl": "https://img.descontovivo.com.br/%s",
                    "imageKey": "%s",
                    "currentPrice": 199.90
                  }]
                }
            """.formatted(sourceId, sourceId, imageKey, imageKey))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true))
            .body("created", is(1))
            .body("errors", empty());
    }

    @Test
    void shouldFailWhenR2PromoteFails() {
        var sourceId = "imgkey-r2fail-" + uid();
        var imageKey = "temp/promotions/2026/07/" + UUID.randomUUID() + ".webp";
        mockR2Storage.setShouldFailOnPromote(true);

        try {
            given()
                .contentType(ContentType.JSON)
                .header("X-Admin-Import-Token", "test-secret-token-123")
                .body("""
                    {
                      "items": [{
                        "sourceId": "%s",
                        "title": "R2 Promote Fail Test",
                        "marketplace": "AMAZON",
                        "storeName": "Amazon",
                        "productUrl": "https://example.com/r2fail-%s",
                        "imageUrl": "https://img.descontovivo.com.br/%s",
                        "imageKey": "%s",
                        "currentPrice": 199.90
                      }]
                    }
                """.formatted(sourceId, sourceId, imageKey, imageKey))
                .when().post(IMPORT_PATH)
                .then()
                .statusCode(200)
                .body("created", is(0))
                .body("errors.size()", is(1))
                .body("errors[0].sourceId", is(sourceId))
                .body("errors[0].field", is("imageKey"))
                .body("errors[0].message", containsString("promover imagem"));
        } finally {
            mockR2Storage.setShouldFailOnPromote(false);
        }
    }

    // --- Helpers ---

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }

    private void importSuccessfully(String sourceId) {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "test-secret-token-123")
            .body(validImportBody(sourceId))
            .when().post(IMPORT_PATH)
            .then()
            .statusCode(200)
            .body("created", is(1));
    }

    private void setPublishedAt(String sourceId, OffsetDateTime publishedAt) {
        QuarkusTransaction.requiringNew().run(() ->
                promotionRepository.update("publishedAt = ?1 where sourceId = ?2", publishedAt, sourceId));
    }

    private String validImportBody(String sourceId) {
        return """
            {
              "batchId": "test-batch",
              "items": [{
                "sourceId": "%s",
                "title": "Test Import %s",
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
