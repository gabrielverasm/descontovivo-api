package br.com.descontovivo.account.api;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record AccountDataRequestCreate(
        @NotNull String type,
        @Size(max = 2000) String details
) {}
