package br.com.descontovivo.account.api;

import java.util.Set;

public record AccountMeResponse(
        String subject,
        String username,
        String email,
        boolean emailVerified,
        Set<String> roles
) {}
