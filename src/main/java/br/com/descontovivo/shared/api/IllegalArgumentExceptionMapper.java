package br.com.descontovivo.shared.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class IllegalArgumentExceptionMapper implements ExceptionMapper<IllegalArgumentException> {

    @Override
    public Response toResponse(IllegalArgumentException e) {
        return Response.status(422)
                .entity(new ApiErrorResponse(422, "Unprocessable Entity", e.getMessage()))
                .build();
    }
}
