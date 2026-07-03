package br.com.descontovivo.account.api.admin;

import br.com.descontovivo.account.service.AccountDataRequestService;
import br.com.descontovivo.shared.api.ApiErrorResponse;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;
import java.util.UUID;

@Path("/api/v1/admin/account/data-requests")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"admin"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Admin - Data Requests", description = "Administrative management of privacy/data requests")
public class AdminDataRequestResource {

    private final AccountDataRequestService dataRequestService;

    public AdminDataRequestResource(AccountDataRequestService dataRequestService) {
        this.dataRequestService = dataRequestService;
    }

    @GET
    @Operation(summary = "List all data requests", description = "Admin-only. Supports filtering by status, type, and user subject.")
    @APIResponse(responseCode = "200", description = "List of data requests")
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public List<AccountDataRequestAdminSummary> list(
            @QueryParam("status") String status,
            @QueryParam("type") String type,
            @QueryParam("userSubject") String userSubject,
            @QueryParam("page") @DefaultValue("0") int page,
            @QueryParam("size") @DefaultValue("20") int size) {
        return dataRequestService.listForAdmin(status, type, userSubject, page, size);
    }

    @PATCH
    @Path("/{id}")
    @Operation(summary = "Update data request status", description = "Admin-only. Transition request status and optionally add resolution note.")
    @APIResponse(responseCode = "200", description = "Request updated")
    @APIResponse(responseCode = "400", description = "Validation error", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "401", description = "Token missing or invalid")
    @APIResponse(responseCode = "403", description = "Insufficient permissions", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    @APIResponse(responseCode = "404", description = "Request not found", content = @Content(schema = @Schema(implementation = ApiErrorResponse.class)))
    public Response updateStatus(@PathParam("id") UUID id,
                                 @Valid AccountDataRequestAdminUpdate request) {
        var updated = dataRequestService.updateStatus(id, request);
        return Response.ok(updated).build();
    }
}
