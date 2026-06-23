package br.com.descontovivo.shared.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/v1/system")
@PermitAll
public class SystemResource {

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    public SystemInfoResponse info() {
        return new SystemInfoResponse("descontovivo-api", "UP");
    }
}
