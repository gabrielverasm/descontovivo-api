package br.com.descontovivo.account.api;

import br.com.descontovivo.shared.security.CurrentUserProvider;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/account")
@Authenticated
@Tag(name = "Account", description = "Current user account information")
@SecurityRequirement(name = "BearerAuth")
public class AccountResource {

    private final CurrentUserProvider currentUserProvider;
    private final SecurityIdentity identity;

    public AccountResource(CurrentUserProvider currentUserProvider, SecurityIdentity identity) {
        this.currentUserProvider = currentUserProvider;
        this.identity = identity;
    }

    @GET
    @Path("/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Get current authenticated user info")
    @APIResponse(responseCode = "200", description = "User info from JWT")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public AccountMeResponse me() {
        var user = currentUserProvider.currentUser();
        return new AccountMeResponse(
                user.subject(),
                user.username(),
                user.email(),
                user.emailVerified(),
                identity.getRoles()
        );
    }
}
