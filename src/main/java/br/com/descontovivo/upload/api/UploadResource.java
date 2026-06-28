package br.com.descontovivo.upload.api;

import br.com.descontovivo.upload.service.PresignService;
import io.quarkus.security.Authenticated;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/uploads")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@Tag(name = "Uploads", description = "Presigned URL generation for image uploads")
public class UploadResource {

    private final PresignService presignService;

    public UploadResource(PresignService presignService) {
        this.presignService = presignService;
    }

    @POST
    @Path("/promotion-image/presign")
    @Authenticated
    @SecurityRequirement(name = "BearerAuth")
    @Operation(summary = "Generate presigned URL for promotion image upload")
    @APIResponse(responseCode = "200", description = "Presigned URL generated")
    @APIResponse(responseCode = "401", description = "Not authenticated")
    @APIResponse(responseCode = "422", description = "Invalid content type or file size")
    public PresignUploadResponse presignPromotionImage(@Valid PresignUploadRequest request) {
        return presignService.generatePresignedUrl(request);
    }
}
