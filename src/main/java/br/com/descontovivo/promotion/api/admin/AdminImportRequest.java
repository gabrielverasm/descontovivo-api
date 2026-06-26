package br.com.descontovivo.promotion.api.admin;

import jakarta.validation.constraints.NotEmpty;

import java.util.List;

public record AdminImportRequest(
        String batchId,
        @NotEmpty List<AdminImportItemRequest> items
) {}
