package br.com.descontovivo.account.api;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.security.TestSecurity;
import io.quarkus.test.security.oidc.Claim;
import io.quarkus.test.security.oidc.ClaimType;
import io.quarkus.test.security.oidc.OidcSecurity;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class AccountResourceTest {

    @Test
    void shouldReturn401WithoutToken() {
        given()
            .when().get("/api/v1/account/me")
            .then()
            .statusCode(401);
    }

    @Test
    @TestSecurity(user = "gabriel", roles = {"user", "moderator"})
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "keycloak-user-sub-123"),
        @Claim(key = "email", value = "gabriel@test.local"),
        @Claim(key = "email_verified", value = "true", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "gabriel")
    })
    void shouldReturnUserInfoWhenAuthenticated() {
        given()
            .when().get("/api/v1/account/me")
            .then()
            .statusCode(200)
            .body("subject", is("keycloak-user-sub-123"))
            .body("username", is("gabriel"))
            .body("email", is("gabriel@test.local"))
            .body("emailVerified", is(true))
            .body("roles", hasItems("user", "moderator"));
    }

    @Test
    @TestSecurity(user = "basic-user", roles = "user")
    @OidcSecurity(claims = {
        @Claim(key = "sub", value = "basic-sub-456"),
        @Claim(key = "email", value = "basic@test.local"),
        @Claim(key = "email_verified", value = "false", type = ClaimType.BOOLEAN),
        @Claim(key = "preferred_username", value = "basic-user")
    })
    void shouldReturnEmailVerifiedFalse() {
        given()
            .when().get("/api/v1/account/me")
            .then()
            .statusCode(200)
            .body("subject", is("basic-sub-456"))
            .body("emailVerified", is(false))
            .body("roles", hasItem("user"))
            .body("roles", not(hasItem("moderator")));
    }
}
