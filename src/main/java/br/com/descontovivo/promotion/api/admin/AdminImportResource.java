package br.com.descontovivo.promotion.api.admin;

import br.com.descontovivo.promotion.service.AdminImportService;
import br.com.descontovivo.shared.security.AdminImportTokenValidator;
import br.com.descontovivo.shared.security.CurrentUserProvider;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin/promotions/import")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Admin Import", description = "Bulk promotion import for editorial content")
public class AdminImportResource {

    private final AdminImportService importService;
    private final AdminImportTokenValidator tokenValidator;
    private final SecurityIdentity identity;
    private final CurrentUserProvider currentUserProvider;

    public AdminImportResource(AdminImportService importService,
                               AdminImportTokenValidator tokenValidator,
                               SecurityIdentity identity,
                               CurrentUserProvider currentUserProvider) {
        this.importService = importService;
        this.tokenValidator = tokenValidator;
        this.identity = identity;
        this.currentUserProvider = currentUserProvider;
    }

    @POST
    @PermitAll
    @Operation(summary = "Import promotions from JSON", description = "Requires admin role or valid X-Admin-Import-Token header")
    public Response importPromotions(@Valid AdminImportRequest request,
                                     @QueryParam("dryRun") @DefaultValue("false") boolean dryRun,
                                     @HeaderParam("X-Admin-Import-Token") String headerToken) {

        boolean hasAdminRole = identity != null && identity.hasRole("admin");
        tokenValidator.requireAdminAccess(hasAdminRole, headerToken);

        String callerUsername = null;
        if (hasAdminRole) {
            try {
                var user = currentUserProvider.currentUser();
                String username = user.username();
                if (username != null && !username.isBlank()) {
                    callerUsername = username;
                }
            } catch (Exception ignored) {
                // sem usuário OIDC resolvível — cai no defaultAuthor
            }
        }

        AdminImportResponse response = dryRun
                ? importService.executeDryRun(request, callerUsername)
                : importService.executePersistent(request, callerUsername);

        return Response.ok(response).build();
    }
}
