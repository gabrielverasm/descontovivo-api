package br.com.descontovivo.promotion.api.admin.backfill;

import br.com.descontovivo.promotion.service.PromotionImageBackfillService;
import br.com.descontovivo.shared.security.AdminImportTokenValidator;
import io.quarkus.security.identity.SecurityIdentity;
import jakarta.annotation.security.PermitAll;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/admin/promotions/images/backfill")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Admin Backfill", description = "Backfill external promotion images to R2")
public class AdminBackfillResource {

    private final PromotionImageBackfillService backfillService;
    private final AdminImportTokenValidator tokenValidator;
    private final SecurityIdentity identity;

    public AdminBackfillResource(PromotionImageBackfillService backfillService,
                                 AdminImportTokenValidator tokenValidator,
                                 SecurityIdentity identity) {
        this.backfillService = backfillService;
        this.tokenValidator = tokenValidator;
        this.identity = identity;
    }

    @POST
    @PermitAll
    @Operation(summary = "Backfill promotion images to R2",
            description = "Finds promotions with external imageUrl and copies images to R2. " +
                    "Requires admin role or valid X-Admin-Import-Token header.")
    public Response backfill(@QueryParam("dryRun") @DefaultValue("true") boolean dryRun,
                             @QueryParam("limit") @DefaultValue("20") int limit,
                             @HeaderParam("X-Admin-Import-Token") String headerToken) {

        boolean hasAdminRole = identity != null && identity.hasRole("admin");
        tokenValidator.requireAdminAccess(hasAdminRole, headerToken);

        if (limit < 1) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("limit deve ser >= 1"))
                    .build();
        }

        if (limit > backfillService.getMaxLimit()) {
            return Response.status(Response.Status.BAD_REQUEST)
                    .entity(new ErrorMessage("limit não pode exceder " + backfillService.getMaxLimit()))
                    .build();
        }

        var response = backfillService.execute(dryRun, limit);
        return Response.ok(response).build();
    }

    private record ErrorMessage(String message) {}
}
