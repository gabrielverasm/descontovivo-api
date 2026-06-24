package br.com.descontovivo.shared.api;

import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IllegalArgumentExceptionMapperTest {

    private final IllegalArgumentExceptionMapper mapper = new IllegalArgumentExceptionMapper();

    @Test
    void shouldReturn422Status() {
        var response = mapper.toResponse(new IllegalArgumentException("invalid value"));
        assertEquals(422, response.getStatus());
    }

    @Test
    void shouldReturnApiErrorResponse() {
        var response = mapper.toResponse(new IllegalArgumentException("invalid value"));
        var body = (ApiErrorResponse) response.getEntity();

        assertEquals(422, body.status());
        assertEquals("Unprocessable Entity", body.error());
        assertEquals("invalid value", body.message());
    }

    @Test
    void shouldNotExposeStacktrace() {
        var response = mapper.toResponse(new IllegalArgumentException("bad input"));
        var body = (ApiErrorResponse) response.getEntity();

        assertFalse(body.message().contains("at br.com."));
        assertFalse(body.message().contains("Exception"));
    }
}
