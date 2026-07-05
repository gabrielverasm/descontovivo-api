package br.com.descontovivo.shared.api;

import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/version")
@PermitAll
@Tag(name = "System", description = "Health and system information")
public class VersionResource {

    @ConfigProperty(name = "app.version", defaultValue = "unknown")
    String version;

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get API version")
    @APIResponse(responseCode = "200", description = "API version info")
    public VersionResponse getVersion() {
        return new VersionResponse("descontovivo-api", version);
    }
}
