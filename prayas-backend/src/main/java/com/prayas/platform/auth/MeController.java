package com.prayas.platform.auth;

import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.user.AppUser;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MeController {

    private final AccessService access;
    private final DomainMembershipRepository memberships;
    private final Environment environment;

    public MeController(AccessService access, DomainMembershipRepository memberships, Environment environment) {
        this.access = access;
        this.memberships = memberships;
        this.environment = environment;
    }

    // No @PreAuthorize needed: @CurrentUser itself throws if there's no
    // logged-in, active app_user -- GlobalExceptionHandler turns that into
    // a clean error response rather than this method ever running with a
    // null user.
    @GetMapping("/api/v1/me")
    public MeView me(@CurrentUser AppUser user, HttpServletRequest request) {
        List<MeView.DomainRoleView> roles = memberships.findByUserId(user.getId()).stream()
                .map(m -> new MeView.DomainRoleView(
                        m.getDomain().getCode(), m.getDomain().getName(), m.getDomainRole()))
                .toList();
        ActingAs actingAs = (ActingAs) request.getAttribute(ActingAs.REQUEST_ATTRIBUTE);
        boolean devTools = environment.acceptsProfiles(Profiles.of("local"));
        return new MeView(
                user.getFullName(),
                user.getEmail(),
                user.getGlobalRole(),
                roles,
                access.canViewTourRequests(),
                access.canDecideTours(),
                access.canReopenTours(),
                access.canAdmin(),
                devTools,
                actingAs);
    }
}
