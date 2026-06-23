package br.com.descontovivo.shared.security;

public record CurrentUser(
        String subject,
        String email,
        String username,
        String name,
        boolean emailVerified
) {}
