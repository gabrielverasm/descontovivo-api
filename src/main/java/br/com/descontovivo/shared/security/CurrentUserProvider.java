package br.com.descontovivo.shared.security;

import io.quarkus.security.identity.SecurityIdentity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.ForbiddenException;
import org.eclipse.microprofile.jwt.JsonWebToken;

@ApplicationScoped
public class CurrentUserProvider {

    private final SecurityIdentity identity;
    private final JsonWebToken jwt;

    public CurrentUserProvider(SecurityIdentity identity, JsonWebToken jwt) {
        this.identity = identity;
        this.jwt = jwt;
    }

    public CurrentUser currentUser() {
        String subject = resolveSubject();
        String email = claimAsString("email");
        String username = claimAsString("preferred_username");
        if (isBlank(username)) {
            username = identity.getPrincipal() != null ? identity.getPrincipal().getName() : null;
        }
        String name = claimAsString("name");
        boolean emailVerified = resolveEmailVerified();

        return new CurrentUser(subject, email, username, name, emailVerified);
    }

    public CurrentUser requireVerifiedUser() {
        var user = currentUser();
        if (!user.emailVerified()) {
            throw new ForbiddenException("Email must be verified to perform this action");
        }
        return user;
    }

    private String resolveSubject() {
        String sub = jwt.getSubject();
        if (!isBlank(sub)) return sub;

        sub = claimAsString("sub");
        if (!isBlank(sub)) return sub;

        if (identity.getPrincipal() != null) {
            String principal = identity.getPrincipal().getName();
            if (!isBlank(principal) && !"anonymous".equals(principal)) {
                return principal;
            }
        }

        throw new ForbiddenException("Authenticated user subject is required");
    }

    private boolean resolveEmailVerified() {
        Object raw = jwt.getClaim("email_verified");
        if (raw instanceof Boolean b) return b;
        if (raw instanceof String s) return "true".equals(s);
        return false;
    }

    private String claimAsString(String name) {
        Object raw = jwt.getClaim(name);
        return raw instanceof String s ? s : null;
    }

    private static boolean isBlank(String s) {
        return s == null || s.isBlank();
    }
}
