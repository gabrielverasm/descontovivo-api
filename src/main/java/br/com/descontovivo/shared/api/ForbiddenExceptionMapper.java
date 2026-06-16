package br.com.descontovivo.shared.api;

import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;
import jakarta.ws.rs.ext.Provider;

@Provider
public class ForbiddenExceptionMapper implements ExceptionMapper<ForbiddenException> {

    @Override
    public Response toResponse(ForbiddenException e) {
        return Response.status(403)
                .entity(new ApiErrorResponse(403, "Forbidden", e.getMessage()))
                .build();
    }
}
