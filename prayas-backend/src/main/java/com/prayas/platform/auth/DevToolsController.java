package com.prayas.platform.auth;

import com.prayas.platform.domain.DomainMembership;
import com.prayas.platform.domain.DomainMembershipRepository;
import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import com.prayas.platform.user.GlobalRole;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * LOCAL PROFILE ONLY. Backs the "Test as" dropdown in the staff UI: lists
 * the staff accounts you can test as, and sets/clears the cookie that
 * DevActAsFilter reads. Every call checks who is REALLY signed in (not who
 * they are currently pretending to be).
 */
@RestController
@Profile("local")
@RequestMapping("/api/v1/dev")
public class DevToolsController {

    private final AppUserRepository users;
    private final DomainMembershipRepository memberships;

    public DevToolsController(AppUserRepository users, DomainMembershipRepository memberships) {
        this.users = users;
        this.memberships = memberships;
    }

    public record DevUser(String email, String fullName, GlobalRole globalRole, List<String> domainRoles) {
    }

    public record ActAsRequest(@NotBlank String email) {
    }

    @GetMapping("/users")
    public List<DevUser> users(HttpServletRequest request) {
        requireRealLeadOrFaculty(request);
        Map<Long, List<String>> rolesByUser = new HashMap<>();
        for (DomainMembership m : memberships.findAllWithDomainAndUser()) {
            rolesByUser.computeIfAbsent(m.getUser().getId(), id -> new ArrayList<>())
                    .add(m.getDomain().getName() + " " + m.getDomainRole());
        }
        return users.findAll(Sort.by("fullName")).stream()
                .filter(AppUser::isActive)
                .map(u -> new DevUser(u.getEmail(), u.getFullName(), u.getGlobalRole(),
                        rolesByUser.getOrDefault(u.getId(), List.of())))
                .toList();
    }

    @PostMapping("/act-as")
    public Map<String, String> actAs(@Valid @RequestBody ActAsRequest body,
                                      HttpServletRequest request, HttpServletResponse response) {
        requireRealLeadOrFaculty(request);
        AppUser target = users.findByEmailIgnoreCase(body.email())
                .filter(AppUser::isActive)
                .orElseThrow(() -> new NoSuchElementException("No active user with that email."));
        Cookie cookie = new Cookie(DevActAsFilter.COOKIE_NAME,
                URLEncoder.encode(target.getEmail(), StandardCharsets.UTF_8));
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(8 * 60 * 60);
        response.addCookie(cookie);
        return Map.of("actingAs", target.getEmail());
    }

    // Deliberately unchecked: clearing the cookie is harmless, and it must
    // work while you are testing as someone who couldn't pass the check above.
    @DeleteMapping("/act-as")
    public void stopActing(HttpServletResponse response) {
        Cookie cookie = new Cookie(DevActAsFilter.COOKIE_NAME, "");
        cookie.setPath("/");
        cookie.setHttpOnly(true);
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    private void requireRealLeadOrFaculty(HttpServletRequest request) {
        Authentication auth = (Authentication) request.getAttribute(DevActAsFilter.REAL_AUTH_ATTRIBUTE);
        if (auth == null) {
            auth = SecurityContextHolder.getContext().getAuthentication();
        }
        if (auth == null || !(auth.getPrincipal() instanceof OAuth2User principal)) {
            throw new AccessDeniedException("Sign in first.");
        }
        String email = principal.getAttribute("email");
        boolean allowed = email != null && users.findByEmailIgnoreCase(email)
                .filter(AppUser::isActive)
                .map(AppUser::isLeadOrFaculty)
                .orElse(false);
        if (!allowed) {
            throw new AccessDeniedException("Only a lead or faculty in-charge can use the test switch.");
        }
    }
}
