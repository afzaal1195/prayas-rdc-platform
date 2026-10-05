package com.prayas.platform.auth;

import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.context.annotation.Profile;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.DefaultOAuth2User;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

/**
 * LOCAL PROFILE ONLY -- this class does not exist as a bean anywhere else.
 *
 * Lets a signed-in Lead/Faculty member test the app as another staff
 * member without juggling several Google accounts: when a "dev_act_as"
 * cookie names an active user, the identity seen by the rest of the request
 * (permission checks, @CurrentUser, /me) is swapped to that user.
 *
 * It swaps the identity for one request only and never writes it back to
 * the session, so dropping the cookie returns you to yourself. Only a REAL
 * Lead/Faculty login can do this, so even on a developer's machine it is
 * not an open impersonation hole.
 */
@Component
@Profile("local")
public class DevActAsFilter extends OncePerRequestFilter {

    public static final String COOKIE_NAME = "dev_act_as";

    /** The genuine Authentication, kept on the request so the dev endpoints can still check who is REALLY signed in. */
    public static final String REAL_AUTH_ATTRIBUTE = "prayas.dev.realAuthentication";

    private final AppUserRepository users;

    public DevActAsFilter(AppUserRepository users) {
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String target = readCookie(request);
        Authentication real = SecurityContextHolder.getContext().getAuthentication();
        if (target != null
                && real instanceof OAuth2AuthenticationToken realToken
                && realToken.getPrincipal() instanceof OAuth2User realUser) {
            swapIdentityIfAllowed(request, realToken, realUser, target);
        }
        chain.doFilter(request, response);
    }

    private void swapIdentityIfAllowed(HttpServletRequest request, OAuth2AuthenticationToken realToken,
                                       OAuth2User realUser, String targetEmail) {
        String realEmail = realUser.getAttribute("email");
        if (realEmail == null) {
            return;
        }
        AppUser realAppUser = users.findByEmailIgnoreCase(realEmail).filter(AppUser::isActive).orElse(null);
        if (realAppUser == null || !realAppUser.isLeadOrFaculty()) {
            return;
        }
        AppUser targetUser = users.findByEmailIgnoreCase(targetEmail).filter(AppUser::isActive).orElse(null);
        if (targetUser == null) {
            return;
        }

        Map<String, Object> attributes = new HashMap<>();
        attributes.put("email", targetUser.getEmail());
        attributes.put("name", targetUser.getFullName());
        OAuth2User standIn = new DefaultOAuth2User(realUser.getAuthorities(), attributes, "email");
        OAuth2AuthenticationToken swapped = new OAuth2AuthenticationToken(
                standIn, standIn.getAuthorities(), realToken.getAuthorizedClientRegistrationId());

        SecurityContext context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(swapped);
        SecurityContextHolder.setContext(context);

        request.setAttribute(REAL_AUTH_ATTRIBUTE, realToken);
        request.setAttribute(ActingAs.REQUEST_ATTRIBUTE,
                new ActingAs(realAppUser.getFullName(), realAppUser.getEmail()));
    }

    private String readCookie(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            return null;
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && cookie.getValue() != null && !cookie.getValue().isBlank()) {
                return URLDecoder.decode(cookie.getValue(), StandardCharsets.UTF_8);
            }
        }
        return null;
    }
}
