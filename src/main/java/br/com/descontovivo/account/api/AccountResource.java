package br.com.descontovivo.account.api;

import br.com.descontovivo.account.service.AccountDataRequestService;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import io.quarkus.security.Authenticated;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/v1/account")
@Authenticated
@Tag(name = "Account", description = "Current user account information")
@SecurityRequirement(name = "BearerAuth")
public class AccountResource {

    private final CurrentUserProvider currentUserProvider;
    private final SecurityIdentity identity;
    private final AccountDataRequestService dataRequestService;

    public AccountResource(CurrentUserProvider currentUserProvider,
                           SecurityIdentity identity,
                           AccountDataRequestService dataRequestService) {
        this.currentUserProvider = currentUserProvider;
        this.identity = identity;
        this.dataRequestService = dataRequestService;
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

    @POST
    @Path("/data-requests")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "Create a data/privacy request (LGPD)")
    @APIResponse(responseCode = "201", description = "Request created")
    @APIResponse(responseCode = "400", description = "Invalid request type")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public Response createDataRequest(@Valid AccountDataRequestCreate request) {
        var response = dataRequestService.create(request.type(), request.details());
        return Response.status(201).entity(response).build();
    }

    @GET
    @Path("/data-requests/me")
    @Produces(MediaType.APPLICATION_JSON)
    @Operation(summary = "List my data requests")
    @APIResponse(responseCode = "200", description = "List of user's data requests")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    public List<AccountDataRequestSummary> myDataRequests() {
        return dataRequestService.listMine();
    }
}
