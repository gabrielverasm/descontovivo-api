package br.com.descontovivo.account.api.admin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountDataRequestAdminUpdate(
        @NotNull String status,
        @Size(max = 2000) String resolutionNote
) {}
