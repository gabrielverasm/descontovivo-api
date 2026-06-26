package br.com.descontovivo.promotion.api.admin;

import java.util.List;

public record AdminImportResponse(
        String batchId,
        boolean dryRun,
        int created,
        int skipped,
        List<AdminImportError> errors
) {}
