package br.com.descontovivo.promotion.inspection;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

import java.util.List;
import java.util.Map;

@Path("/api/v1/admin/shopee/affiliate-operation")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@RolesAllowed({"admin", "moderator"})
public class ShopeeAffiliateOperationResource {
    private final ShopeeAffiliateOperationService service;

    public ShopeeAffiliateOperationResource(ShopeeAffiliateOperationService service) {
        this.service = service;
    }

    @POST
    public Response execute(@Valid Request request) {
        try {
            JsonNode result = service.execute(new ShopeeAffiliateOperationService.ShopeeAffiliateOperationRequest(
                    request.operation(), request.keyword(), request.page(), request.limit(), request.startTime(),
                    request.endTime(), request.purchaseStatus(), request.feedId(), request.originUrl(), request.subIds()));
            return Response.ok(result).build();
        } catch (MarketplaceInspectionException e) {
            int status = switch (e.code()) {
                case "INVALID_OPERATION" -> 400;
                case "AFFILIATE_API_RATE_LIMIT" -> 429;
                case "IMPORTER_UNAVAILABLE" -> 503;
                default -> 502;
            };
            return Response.status(status).entity(Map.of("code", e.code(), "message", e.getMessage())).build();
        }
    }

    public record Request(
            @NotBlank String operation,
            String keyword,
            @Min(1) @Max(10000) int page,
            @Min(1) @Max(100) int limit,
            Long startTime,
            Long endTime,
            Integer purchaseStatus,
            Long feedId,
            String originUrl,
            List<String> subIds) {
        public Request {
            subIds = subIds == null ? List.of() : List.copyOf(subIds);
        }
    }
}
