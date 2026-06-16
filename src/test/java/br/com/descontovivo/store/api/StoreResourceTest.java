package br.com.descontovivo.store.api;

import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.*;

@QuarkusTest
class StoreResourceTest {

    @Test
    void shouldListStores() {
        given()
            .when().get("/api/v1/stores")
            .then()
            .statusCode(200)
            .body("$.size()", greaterThanOrEqualTo(3))
            .body("slug", hasItems("amazon", "mercado-livre", "magalu"));
    }

    @Test
    void shouldGetStoreBySlug() {
        given()
            .when().get("/api/v1/stores/amazon")
            .then()
            .statusCode(200)
            .body("slug", is("amazon"))
            .body("name", is("Amazon"))
            .body("id", notNullValue());
    }

    @Test
    void shouldReturn404ForUnknownSlug() {
        given()
            .when().get("/api/v1/stores/unknown-store")
            .then()
            .statusCode(404);
    }
}
