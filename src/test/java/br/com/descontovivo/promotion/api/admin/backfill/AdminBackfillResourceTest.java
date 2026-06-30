package br.com.descontovivo.promotion.api.admin.backfill;

import br.com.descontovivo.promotion.entity.OfferAvailability;
import br.com.descontovivo.promotion.entity.PromotionEntity;
import br.com.descontovivo.promotion.entity.PromotionStatus;
import br.com.descontovivo.promotion.repository.PromotionRepository;
import br.com.descontovivo.store.entity.StoreEntity;
import br.com.descontovivo.store.repository.StoreRepository;
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
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Map;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

@QuarkusTest
@TestProfile(AdminBackfillResourceTest.Profile.class)
class AdminBackfillResourceTest {

    public static class Profile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return Map.of(
                "admin.import.token", "test-secret-token-123",
                "r2.public-base-url", "https://img.descontovivo.com.br"
            );
        }
    }

    private static final String BACKFILL_PATH = "/api/v1/admin/promotions/images/backfill";
    private static final String TOKEN = "test-secret-token-123";

    @Inject
    PromotionRepository promotionRepository;

    @Inject
    StoreRepository storeRepository;

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
            .when().post(BACKFILL_PATH)
            .then().statusCode(403);
    }

    @Test
    void shouldReturn403WithInvalidToken() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", "wrong-token")
            .when().post(BACKFILL_PATH)
            .then().statusCode(403);
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
            .when().post(BACKFILL_PATH)
            .then().statusCode(403);
    }

    @Test
    void shouldAllowAccessWithValidToken() {
        given()
            .contentType(ContentType.JSON)
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true));
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
            .queryParam("dryRun", true)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true));
    }

    // --- Dry run tests ---

    @Test
    void shouldDryRunListEligibleWithoutUpdating() {
        var promo = createPromotionWithExternalImage("dryrun-list-" + uid());

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true))
            .body("eligible", greaterThanOrEqualTo(1))
            .body("updated", is(0))
            .body("failed", is(0))
            .body("items.find { it.promotionId == '%s' }.status".formatted(promo.getId()), is("ELIGIBLE"))
            .body("items.find { it.promotionId == '%s' }.oldImageUrl".formatted(promo.getId()),
                    startsWith("https://m.media-amazon.com/"));

        // Run dry run again - should still find same promos (nothing was updated)
        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("eligible", greaterThanOrEqualTo(1))
            .body("items.find { it.promotionId == '%s' }".formatted(promo.getId()), notNullValue());
    }

    @Test
    void shouldDryRunNotCallImportImage() {
        createPromotionWithExternalImage("dryrun-noimport-" + uid());
        mockImageImport.clearImportedUrls();

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true));

        assertTrue(mockImageImport.getImportedUrls().isEmpty(),
                "Dry run should NOT trigger image import");
    }

    // --- Real execution tests ---

    @Test
    void shouldUpdateExternalImageToR2() {
        var promo = createPromotionWithExternalImage("real-update-" + uid());

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", false)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(false))
            .body("updated", greaterThanOrEqualTo(1))
            .body("items.find { it.promotionId == '%s' }.status".formatted(promo.getId()), is("UPDATED"))
            .body("items.find { it.promotionId == '%s' }.newImageUrl".formatted(promo.getId()),
                    startsWith("https://img.descontovivo.com.br/promotions/imported/"))
            .body("items.find { it.promotionId == '%s' }.imageKey".formatted(promo.getId()),
                    startsWith("promotions/imported/"))
            .body("items.find { it.promotionId == '%s' }.oldImageUrl".formatted(promo.getId()),
                    startsWith("https://m.media-amazon.com/"));
    }

    @Test
    void shouldFillImageKeyAfterBackfill() {
        var promo = createPromotionWithExternalImage("fill-key-" + uid());

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", false)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("updated", greaterThanOrEqualTo(1))
            .body("items.find { it.promotionId == '%s' }.imageKey".formatted(promo.getId()),
                    allOf(notNullValue(), startsWith("promotions/imported/"), endsWith(".webp")));
    }

    // --- R2 images should not be reprocessed ---

    @Test
    void shouldNotAlterPromotionAlreadyOnR2() {
        var promo = createPromotionWithR2Image("already-r2-" + uid());

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", false)
            .queryParam("limit", 100)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("items.find { it.promotionId == '%s' }".formatted(promo.getId()), nullValue());
    }

    // --- Error handling ---

    @Test
    void shouldProcessAllEligibleItems() {
        createPromotionWithExternalImage("process-1-" + uid());
        createPromotionWithExternalImage("process-2-" + uid());

        mockImageImport.reset();

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", false)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(false))
            .body("updated", greaterThanOrEqualTo(2))
            .body("failed", is(0))
            .body("items.findAll { it.status == 'UPDATED' }.size()", greaterThanOrEqualTo(2));
    }

    @Test
    void shouldFailOneItemWithoutStoppingBatch() {
        // Create two promotions with external images
        createPromotionWithExternalImage("batch-fail-1-" + uid());
        createPromotionWithExternalImage("batch-fail-2-" + uid());

        // Make mock fail for all (simulates network issues)
        mockImageImport.setShouldFail(true);
        mockImageImport.setFailureMessage("Connection timeout");

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", false)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(false))
            .body("failed", greaterThanOrEqualTo(2))
            .body("updated", is(0))
            .body("items.findAll { it.status == 'FAILED' }.size()", greaterThanOrEqualTo(2))
            .body("items.findAll { it.status == 'FAILED' }[0].error", is("Connection timeout"));

        // After failure, if we reset mock and run again, those same promos should still be eligible
        mockImageImport.reset();

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 50)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("eligible", greaterThanOrEqualTo(2));
    }

    // --- Limit tests ---

    @Test
    void shouldRespectLimit() {
        // Create 3 promotions
        createPromotionWithExternalImage("limit-1-" + uid());
        createPromotionWithExternalImage("limit-2-" + uid());
        createPromotionWithExternalImage("limit-3-" + uid());

        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 1)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("eligible", is(1))
            .body("items.size()", is(1));
    }

    @Test
    void shouldRejectLimitAboveMax() {
        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 200)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(400)
            .body("message", containsString("limit não pode exceder"));
    }

    @Test
    void shouldRejectLimitBelowOne() {
        given()
            .header("X-Admin-Import-Token", TOKEN)
            .queryParam("dryRun", true)
            .queryParam("limit", 0)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(400)
            .body("message", containsString("limit deve ser >= 1"));
    }

    // --- Default parameters ---

    @Test
    void shouldDefaultDryRunToTrue() {
        given()
            .header("X-Admin-Import-Token", TOKEN)
            .when().post(BACKFILL_PATH)
            .then()
            .statusCode(200)
            .body("dryRun", is(true));
    }

    // --- Helpers ---

    @Transactional
    PromotionEntity createPromotionWithExternalImage(String suffix) {
        return createPromotionWithExternalImage(suffix,
                "https://m.media-amazon.com/images/I/" + suffix + ".jpg");
    }

    @Transactional
    PromotionEntity createPromotionWithExternalImage(String suffix, String imageUrl) {
        var store = getOrCreateStore();
        var now = OffsetDateTime.now();

        var entity = new PromotionEntity();
        entity.setSlug("backfill-test-" + suffix);
        entity.setTitle("Backfill Test " + suffix);
        entity.setUrl("https://example.com/" + suffix);
        entity.setNormalizedUrl("example.com/" + suffix);
        entity.setDescription("Test promotion for backfill " + suffix);
        entity.setNormalizedDescription("test promotion for backfill " + suffix);
        entity.setCurrentPrice(BigDecimal.valueOf(99.90));
        entity.setImageUrl(imageUrl);
        entity.setImageKey(null);
        entity.setStatus(PromotionStatus.PUBLISHED);
        entity.setAvailability(OfferAvailability.AVAILABLE);
        entity.setStore(store);
        entity.setCreatedDate(LocalDate.now());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setPublishedAt(now);
        entity.setPublishAt(now);
        entity.setSource("BACKFILL_TEST");
        entity.setSourceId("backfill-test-" + suffix);

        promotionRepository.persist(entity);
        return entity;
    }

    @Transactional
    PromotionEntity createPromotionWithR2Image(String suffix) {
        var store = getOrCreateStore();
        var now = OffsetDateTime.now();

        var entity = new PromotionEntity();
        entity.setSlug("r2-test-" + suffix);
        entity.setTitle("R2 Image Test " + suffix);
        entity.setUrl("https://example.com/r2-" + suffix);
        entity.setNormalizedUrl("example.com/r2-" + suffix);
        entity.setDescription("Already on R2 " + suffix);
        entity.setNormalizedDescription("already on r2 " + suffix);
        entity.setCurrentPrice(BigDecimal.valueOf(149.90));
        entity.setImageUrl("https://img.descontovivo.com.br/promotions/imported/2025/07/" + suffix + ".webp");
        entity.setImageKey("promotions/imported/2025/07/" + suffix + ".webp");
        entity.setStatus(PromotionStatus.PUBLISHED);
        entity.setAvailability(OfferAvailability.AVAILABLE);
        entity.setStore(store);
        entity.setCreatedDate(LocalDate.now());
        entity.setCreatedAt(now);
        entity.setUpdatedAt(now);
        entity.setPublishedAt(now);
        entity.setPublishAt(now);
        entity.setSource("BACKFILL_TEST");
        entity.setSourceId("r2-test-" + suffix);

        promotionRepository.persist(entity);
        return entity;
    }

    private StoreEntity getOrCreateStore() {
        return storeRepository.findBySlug("backfill-test-store")
                .orElseGet(() -> {
                    var store = new StoreEntity();
                    store.setName("Backfill Test Store");
                    store.setSlug("backfill-test-store");
                    store.setCreatedAt(java.time.LocalDateTime.now());
                    storeRepository.persist(store);
                    return store;
                });
    }

    private static String uid() {
        return UUID.randomUUID().toString().substring(0, 8);
    }
}
