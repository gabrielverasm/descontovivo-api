package br.com.descontovivo.moderation.api;

import br.com.descontovivo.moderation.service.ModerationCategoryService;
import jakarta.annotation.security.RolesAllowed;
import jakarta.validation.Valid;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/v1/moderation/categories")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
@RolesAllowed({"moderator", "admin"})
@SecurityRequirement(name = "BearerAuth")
@Tag(name = "Moderation", description = "Category management for moderators/admins")
public class ModerationCategoryResource {

    private final ModerationCategoryService categoryService;

    public ModerationCategoryResource(ModerationCategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GET
    @Operation(summary = "List all categories with promotion count")
    public Response list() {
        return Response.ok(categoryService.listCategories()).build();
    }

    @PATCH
    @Path("/{categoryName}")
    @Operation(summary = "Rename a category")
    public Response rename(@PathParam("categoryName") String categoryName,
                           @Valid RenameCategoryRequest request) {
        return Response.ok(categoryService.renameCategory(categoryName, request)).build();
    }

    @DELETE
    @Path("/{categoryName}")
    @Operation(summary = "Delete a category (clears from all promotions)")
    public Response delete(@PathParam("categoryName") String categoryName) {
        categoryService.deleteCategory(categoryName);
        return Response.noContent().build();
    }
}
