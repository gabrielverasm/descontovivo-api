package br.com.descontovivo.shared.api;

import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ConflictExceptionMapper implements ExceptionMapper<ConflictException> {

    @Override
    public Response toResponse(ConflictException e) {
        return Response.status(409)
                .entity(new ApiErrorResponse(409, "Conflict", e.getMessage()))
                .build();
    }
}
