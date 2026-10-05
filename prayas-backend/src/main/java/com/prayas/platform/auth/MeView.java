package com.prayas.platform.auth;

import com.prayas.platform.domain.DomainRole;
import com.prayas.platform.user.GlobalRole;

import java.util.List;

/**
 * Response shape for GET /api/v1/me -- who's logged in, what they can do,
 * and (for the local test switch) whether they're currently testing as
 * someone else. The booleans only drive what the UI shows; every endpoint
 * still enforces its own permission on the server.
 */
public record MeView(
        String name,
        String email,
        GlobalRole globalRole,
        List<DomainRoleView> domainRoles,
        boolean canViewTours,
        boolean canDecide,
        boolean canReopen,
        boolean canAdmin,
        boolean devTools,
        ActingAs actingAs
) {
    public record DomainRoleView(String domainCode, String domainName, DomainRole role) {
    }
}
