package br.com.descontovivo.shared.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.is;

@QuarkusTest
class SystemResourceTest {

    @Test
    void shouldReturnSystemInfo() {
        given()
            .when().get("/api/v1/system/info")
            .then()
            .statusCode(200)
            .body("application", is("descontovivo-api"))
            .body("status", is("UP"));
    }
}
