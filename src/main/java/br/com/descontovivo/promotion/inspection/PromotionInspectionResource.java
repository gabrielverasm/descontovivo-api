package br.com.descontovivo.promotion.inspection;

import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.Map;

@Path("/api/v1/admin/promotions/inspect-url")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"admin", "moderator"})
public class PromotionInspectionResource {
    private static final Map<String, Integer> ERROR_STATUS = Map.of(
            "INVALID_URL", 400,
            "UNSUPPORTED_MARKETPLACE", 422,
            "INSPECTION_BLOCKED", 422,
            "INSPECTION_FAILED", 422,
            "IMPORTER_UNAVAILABLE", 503);
    private final PromotionInspectionService service;

    public PromotionInspectionResource(PromotionInspectionService service) { this.service = service; }

    @POST
    public Response inspect(@Valid PromotionInspectionRequest request) {
        try {
            return Response.ok(service.inspect(request.url())).build();
        } catch (MarketplaceInspectionException e) {
            int status = ERROR_STATUS.getOrDefault(e.code(), 422);
            return Response.status(status).entity(new ErrorResponse(e.code(), e.getMessage())).build();
        }
    }

    public record ErrorResponse(String code, String message) {}
}
