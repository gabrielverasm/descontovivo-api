package br.com.descontovivo.shared.security;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class AdminImportTokenValidator {

    @ConfigProperty(name = "admin.import.token")
    Optional<String> configuredToken;

    public boolean isValidToken(String headerToken) {
        if (configuredToken.isEmpty() || configuredToken.get().isBlank()) return false;
        if (headerToken == null || headerToken.isBlank()) return false;
        return configuredToken.get().equals(headerToken);
    }

    public void requireAdminAccess(boolean hasAdminRole, String headerToken) {
        if (hasAdminRole) return;
        if (isValidToken(headerToken)) return;
        throw new ForbiddenException("Admin access required");
    }
}
