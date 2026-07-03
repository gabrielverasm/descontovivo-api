package br.com.descontovivo.account.api.admin;

import br.com.descontovivo.account.entity.AccountDataRequestEntity;
import br.com.descontovivo.account.entity.DataRequestStatus;
import br.com.descontovivo.account.entity.DataRequestType;
import br.com.descontovivo.account.repository.AccountDataRequestRepository;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.OidcSecurity;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.OffsetDateTime;
import java.util.UUID;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AdminDataRequestResourceTest {

    private static final String ADMIN_PATH = "/api/v1/admin/account/data-requests";
    private static final String USER_PATH = "/api/v1/account/data-requests";
    private static final String USER_ME_PATH = "/api/v1/account/data-requests/me";

    @Inject
    AccountDataRequestRepository dataRequestRepository;

    // ========== User flow tests ==========

    @Test
    @TestSecurity(user = "privacy-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "privacy-user-sub"),
        @Claim(key = "email", value = "privacy@test.local"),
        @Claim(key = "preferred_username", value = "privacy-user")
    })
    void userCanCreateDataRequestWithValidType() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "ACCESS", "details": "Quero saber quais dados estão armazenados." }
            """)
            .when().post(USER_PATH)
            .then()
            .statusCode(201)
            .body("id", notNullValue())
            .body("type", is("ACCESS"))
            .body("status", is("PENDING"))
            .body("createdAt", notNullValue())
            .body("message", notNullValue());
    }

    @Test
    @TestSecurity(user = "privacy-user2", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "privacy-user2-sub"),
        @Claim(key = "email", value = "privacy2@test.local"),
        @Claim(key = "preferred_username", value = "privacy-user2")
    })
    void userCanCreateDataRequestWithNullDetails() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "DELETION" }
            """)
            .when().post(USER_PATH)
            .then()
            .statusCode(201)
            .body("type", is("DELETION"))
            .body("status", is("PENDING"));
    }

    @Test
    @TestSecurity(user = "privacy-user3", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "privacy-user3-sub"),
        @Claim(key = "email", value = "privacy3@test.local"),
        @Claim(key = "preferred_username", value = "privacy-user3")
    })
    void userCannotCreateDataRequestWithInvalidType() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "INVALID_TYPE", "details": "algo" }
            """)
            .when().post(USER_PATH)
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "privacy-user4", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "privacy-user4-sub"),
        @Claim(key = "email", value = "privacy4@test.local"),
        @Claim(key = "preferred_username", value = "privacy-user4")
    })
    void userCannotCreateDataRequestWithoutType() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "details": "sem tipo" }
            """)
            .when().post(USER_PATH)
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "privacy-lister-a", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "privacy-user-a-sub"),
        @Claim(key = "email", value = "usera@test.local"),
        @Claim(key = "preferred_username", value = "privacy-lister-a")
    })
    void userCanListOnlyOwnRequests() {
        // Seed a request directly in DB for user B (different sub)
        seedRequestForUserB();

        // Create a request as user A (this user)
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "CONSENT_REVOCATION", "details": "Solicitação do usuário A" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201);

        // List as user A — must see own, must NOT see user B's
        given()
            .when().get(USER_ME_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("type", hasItem("CONSENT_REVOCATION"))
            .body("type", not(hasItem("DELETION")));
    }

    @Transactional
    void seedRequestForUserB() {
        var entity = new AccountDataRequestEntity();
        entity.setUserSubject("privacy-user-b-sub");
        entity.setUsername("privacy-lister-b");
        entity.setEmail("userb@test.local");
        entity.setDisplayName("User B");
        entity.setRequestType(DataRequestType.DELETION);
        entity.setDetails("Solicitação do usuário B");
        entity.setStatus(DataRequestStatus.PENDING);
        entity.setCreatedAt(OffsetDateTime.now());
        dataRequestRepository.persist(entity);
    }

    // ========== Admin flow tests ==========

    @Test
    @TestSecurity(user = "admin-dr", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-dr-sub"),
        @Claim(key = "email", value = "admin-dr@test.local"),
        @Claim(key = "preferred_username", value = "admin-dr")
    })
    void adminCanListAllDataRequests() {
        // First create a request as this user (for test data)
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "ACCESS", "details": "Admin listing test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201);

        // Then list as admin
        given()
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("[0].id", notNullValue())
            .body("[0].userSubject", notNullValue())
            .body("[0].status", notNullValue());
    }

    @Test
    @TestSecurity(user = "admin-filter", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-filter-sub"),
        @Claim(key = "email", value = "admin-filter@test.local"),
        @Claim(key = "preferred_username", value = "admin-filter")
    })
    void adminCanFilterByStatus() {
        // Create a PENDING request
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "CORRECTION", "details": "Filter test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201);

        // Filter by PENDING
        given()
            .queryParam("status", "PENDING")
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("status", everyItem(is("PENDING")));
    }

    @Test
    @TestSecurity(user = "admin-update", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-update-sub"),
        @Claim(key = "email", value = "admin-update@test.local"),
        @Claim(key = "preferred_username", value = "admin-update")
    })
    void adminCanChangeStatusToInReview() {
        // Create request
        String id = given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "ANONYMIZATION", "details": "Status change test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201)
            .extract().jsonPath().getString("id");

        // Change to IN_REVIEW
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "IN_REVIEW" }
            """)
            .when().patch(ADMIN_PATH + "/" + id)
            .then()
            .statusCode(200)
            .body("status", is("IN_REVIEW"))
            .body("updatedAt", notNullValue())
            .body("resolvedAt", nullValue());
    }

    @Test
    @TestSecurity(user = "admin-complete", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-complete-sub"),
        @Claim(key = "email", value = "admin-complete@test.local"),
        @Claim(key = "preferred_username", value = "admin-complete")
    })
    void adminCanCompleteWithResolutionNote() {
        // Create request
        String id = given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "ACCESS", "details": "Completion test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201)
            .extract().jsonPath().getString("id");

        // Complete with note
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "COMPLETED", "resolutionNote": "Dados enviados por e-mail." }
            """)
            .when().patch(ADMIN_PATH + "/" + id)
            .then()
            .statusCode(200)
            .body("status", is("COMPLETED"))
            .body("updatedAt", notNullValue())
            .body("resolvedAt", notNullValue())
            .body("resolutionNote", is("Dados enviados por e-mail."));
    }

    @Test
    @TestSecurity(user = "admin-reject", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-reject-sub"),
        @Claim(key = "email", value = "admin-reject@test.local"),
        @Claim(key = "preferred_username", value = "admin-reject")
    })
    void adminCannotRevertTerminalStatus() {
        // Create and complete
        String id = given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "DELETION", "details": "Terminal test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201)
            .extract().jsonPath().getString("id");

        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "REJECTED", "resolutionNote": "Dados necessários para obrigação legal." }
            """)
            .when().patch(ADMIN_PATH + "/" + id)
            .then().statusCode(200)
            .body("status", is("REJECTED"))
            .body("resolvedAt", notNullValue());

        // Try to revert to PENDING — should fail
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "PENDING" }
            """)
            .when().patch(ADMIN_PATH + "/" + id)
            .then()
            .statusCode(400);
    }

    @Test
    @TestSecurity(user = "admin-404", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-404-sub"),
        @Claim(key = "email", value = "admin-404@test.local"),
        @Claim(key = "preferred_username", value = "admin-404")
    })
    void adminGets404ForNonexistentId() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "IN_REVIEW" }
            """)
            .when().patch(ADMIN_PATH + "/" + UUID.randomUUID())
            .then()
            .statusCode(404);
    }

    // ========== Edge cases & additional filters ==========

    @Test
    @TestSecurity(user = "admin-page-edge", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-page-edge-sub"),
        @Claim(key = "email", value = "admin-page-edge@test.local"),
        @Claim(key = "preferred_username", value = "admin-page-edge")
    })
    void adminListHandlesNegativePageAndZeroSize() {
        // Negative page and size=0 should not cause 500/422
        given()
            .queryParam("page", -1)
            .queryParam("size", 0)
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200);
    }

    @Test
    @TestSecurity(user = "admin-type-filter", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-type-filter-sub"),
        @Claim(key = "email", value = "admin-type-filter@test.local"),
        @Claim(key = "preferred_username", value = "admin-type-filter")
    })
    void adminCanFilterByType() {
        // Create a request with type OTHER
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "OTHER", "details": "Filter by type test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201);

        // Filter by type=OTHER
        given()
            .queryParam("type", "OTHER")
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("type", everyItem(is("OTHER")));
    }

    @Test
    @TestSecurity(user = "admin-subject-filter", roles = {"user", "admin"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "admin-subject-filter-sub"),
        @Claim(key = "email", value = "admin-subject-filter@test.local"),
        @Claim(key = "preferred_username", value = "admin-subject-filter")
    })
    void adminCanFilterByUserSubject() {
        // Create a request (will be created with sub admin-subject-filter-sub)
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "type": "ACCESS", "details": "Subject filter test" }
            """)
            .when().post(USER_PATH)
            .then().statusCode(201);

        // Filter by this user's subject
        given()
            .queryParam("userSubject", "admin-subject-filter-sub")
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(1))
            .body("userSubject", everyItem(is("admin-subject-filter-sub")));

        // Filter by non-existent subject should return empty
        given()
            .queryParam("userSubject", "non-existent-subject-xyz")
            .when().get(ADMIN_PATH)
            .then()
            .statusCode(200)
            .body("$.size()", is(0));
    }

    // ========== Security tests ==========

    @Test
    void shouldReturn401WithoutAuthOnAdminEndpoints() {
        given()
            .when().get(ADMIN_PATH)
            .then().statusCode(401);
    }

    @Test
    @TestSecurity(user = "regular-user-noadmin", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "regular-noadmin-sub"),
        @Claim(key = "email", value = "regular@test.local"),
        @Claim(key = "preferred_username", value = "regular-user-noadmin")
    })
    void regularUserCannotAccessAdminList() {
        given()
            .when().get(ADMIN_PATH)
            .then().statusCode(403);
    }

    @Test
    @TestSecurity(user = "regular-user-noadmin2", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "regular-noadmin2-sub"),
        @Claim(key = "email", value = "regular2@test.local"),
        @Claim(key = "preferred_username", value = "regular-user-noadmin2")
    })
    void regularUserCannotUpdateStatus() {
        given()
            .contentType(ContentType.JSON)
            .body("""
                { "status": "IN_REVIEW" }
            """)
            .when().patch(ADMIN_PATH + "/" + UUID.randomUUID())
            .then().statusCode(403);
    }
}
