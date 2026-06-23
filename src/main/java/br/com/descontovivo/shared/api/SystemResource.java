package br.com.descontovivo.shared.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/system")
@PermitAll
@Tag(name = "System", description = "Health and system information")
public class SystemResource {

    @GET
    @Path("/info")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get system info")
    @APIResponse(responseCode = "200", description = "System status")
    public SystemInfoResponse info() {
        return new SystemInfoResponse("descontovivo-api", "UP");
    }
}
