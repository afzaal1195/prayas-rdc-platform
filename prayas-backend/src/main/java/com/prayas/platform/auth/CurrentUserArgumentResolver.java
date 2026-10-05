package com.prayas.platform.auth;

import com.prayas.platform.user.AppUser;
import com.prayas.platform.user.AppUserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.NoSuchElementException;

/**
 * Resolves an @CurrentUser AppUser parameter from the logged-in Google
 * session. Registered in WebMvcConfig via addArgumentResolvers(...).
 *
 * The email lookup is the single point where "logged in with Google" and
 * "known to app_user" meet: an institute email that authenticates but has
 * no app_user row is treated as not logged in, since staff accounts are
 * provisioned by an admin, not created on first login.
 */
@Component
public class CurrentUserArgumentResolver implements HandlerMethodArgumentResolver {

    private final AppUserRepository appUserRepository;

    public CurrentUserArgumentResolver(AppUserRepository appUserRepository) {
        this.appUserRepository = appUserRepository;
    }

    @Override
    public boolean supportsParameter(MethodParameter parameter) {
        return parameter.hasParameterAnnotation(CurrentUser.class)
                && AppUser.class.isAssignableFrom(parameter.getParameterType());
    }

    @Override
    public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer,
                                   NativeWebRequest webRequest, WebDataBinderFactory binderFactory) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof OAuth2User oauthUser)) {
            throw new IllegalStateException("No authenticated OAuth2 user in context");
        }
        String email = oauthUser.getAttribute("email");
        if (email == null) {
            throw new IllegalStateException("Google profile did not include an email");
        }
        return appUserRepository.findByEmailIgnoreCase(email)
                .filter(AppUser::isActive)
                .orElseThrow(() -> new NoSuchElementException(
                        "No active app_user for " + email + " -- ask an admin to add this account"));
    }
}
