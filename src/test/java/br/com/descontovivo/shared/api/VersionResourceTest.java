package br.com.descontovivo.shared.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class VersionResourceTest {

    @Test
    void shouldReturnVersionInfo() {
        given()
            .when().get("/api/v1/version")
            .then()
            .statusCode(200)
            .body("name", is("descontovivo-api"))
            .body("version", is("0.2.1"));
    }

    @Test
    void shouldBePublicEndpoint() {
        // No auth header — should still return 200
        given()
            .when().get("/api/v1/version")
            .then()
            .statusCode(200);
    }
}
