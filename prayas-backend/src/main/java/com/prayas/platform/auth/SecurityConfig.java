package com.prayas.platform.auth;

import com.prayas.platform.tour.PublicSubmitRateLimitFilter;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.oauth2.client.registration.ClientRegistrationRepository;
import org.springframework.security.oauth2.client.userinfo.DefaultOAuth2UserService;
import org.springframework.security.oauth2.client.userinfo.OAuth2UserRequest;
import org.springframework.security.oauth2.client.web.DefaultOAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.client.web.OAuth2AuthorizationRequestResolver;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.context.SecurityContextHolderFilter;
import org.springframework.security.web.util.matcher.AntPathRequestMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

/**
 * Two audiences, two trust levels:
 *  - /api/v1/public/**  no login; protected by captcha + honeypot + rate
 *    limit in the service layer instead (see PublicSubmitRateLimitFilter).
 *  - everything else    Google OAuth2 login, restricted below to accounts
 *    on the institute's email domain. A restricted-domain login still
 *    needs a matching app_user row (see CurrentUserArgumentResolver) --
 *    domain-matching only keeps random Gmail accounts out, it does not
 *    grant access by itself.
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final EmailDomainPolicy emailDomainPolicy;

    public SecurityConfig(EmailDomainPolicy emailDomainPolicy) {
        this.emailDomainPolicy = emailDomainPolicy;
    }

    @Value("${prayas.auth.allowed-email-domain}")
    private String allowedEmailDomain;

    @Value("${prayas.frontend.origin}")
    private String frontendOrigin;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http, PublicSubmitRateLimitFilter rateLimitFilter,
                                            ClientRegistrationRepository clientRegistrationRepository,
                                            ObjectProvider<DevActAsFilter> devActAsFilter)
            throws Exception {
        http
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .csrf(csrf -> csrf.ignoringRequestMatchers("/api/v1/**"))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/public/**", "/actuator/health", "/error").permitAll()
                .anyRequest().authenticated())
            .exceptionHandling(ex -> ex.defaultAuthenticationEntryPointFor(
                    apiUnauthorizedEntryPoint(), new AntPathRequestMatcher("/api/v1/**")))
            .oauth2Login(oauth2 -> oauth2
                .authorizationEndpoint(endpoint -> endpoint
                        .authorizationRequestResolver(accountPickerResolver(clientRegistrationRepository)))
                .defaultSuccessUrl(frontendOrigin + "/staff", true)
                // A rejected login (wrong domain, no app_user row, etc.) lands
                // back on our own login screen with a query flag instead of
                // Spring's default whitelabel "/login?error" page, which has
                // no path back into the app.
                .failureUrl(frontendOrigin + "/staff?error=access_denied")
                .userInfoEndpoint(userInfo -> userInfo.userService(domainRestrictedUserService())))
            .logout(logout -> logout
                // A plain <a href="/logout"> link sends a GET; Spring's
                // default only matches POST. GET is a "safe" method that
                // CSRF protection never applies to anyway, so this doesn't
                // need adding to the CSRF-ignore list.
                .logoutRequestMatcher(new AntPathRequestMatcher("/logout", "GET"))
                .logoutSuccessUrl(frontendOrigin + "/staff")
                // Also forget any local "test as" choice, so it can't silently
                // re-apply at the next sign-in. (A no-op cookie name everywhere else.)
                .deleteCookies(DevActAsFilter.COOKIE_NAME))
            .addFilterBefore(rateLimitFilter, UsernamePasswordAuthenticationFilter.class);
        // Only present under the "local" profile -- a no-op everywhere else.
        devActAsFilter.ifAvailable(filter -> http.addFilterAfter(filter, SecurityContextHolderFilter.class));
        return http.build();
    }

    /**
     * The default OAuth2 entry point redirects an unauthenticated request to
     * Google's login page -- fine for a browser navigating there directly,
     * useless for a fetch() call from the React app checking "am I logged
     * in?" (a redirect through Google isn't something fetch can usefully
     * follow). API requests get a clean 401 instead, so the frontend can
     * show its own login button.
     */
    private org.springframework.security.web.AuthenticationEntryPoint apiUnauthorizedEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json");
            response.getWriter().write("{\"detail\":\"Not logged in.\"}");
        };
    }

    /**
     * Without this, once a browser has authenticated with Google once,
     * clicking "Sign in with Google" again silently retries that same
     * account with no picker shown -- so a rejected non-institute account
     * looked like it was "stuck" with no way to choose a different one.
     * Forcing prompt=select_account makes Google show the account chooser
     * on every attempt.
     */
    private OAuth2AuthorizationRequestResolver accountPickerResolver(ClientRegistrationRepository repo) {
        DefaultOAuth2AuthorizationRequestResolver resolver =
                new DefaultOAuth2AuthorizationRequestResolver(repo, "/oauth2/authorization");
        resolver.setAuthorizationRequestCustomizer(customizer ->
                customizer.additionalParameters(params -> params.put("prompt", "select_account")));
        return resolver;
    }

    /**
     * The React dev server runs on a different origin (port) than this API,
     * so the browser blocks cross-origin requests unless we explicitly allow
     * it here. allowCredentials(true) is needed because staff endpoints rely
     * on the session cookie -- which means the origin can't be a wildcard,
     * it has to be this exact configured value.
     */
    private CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(List.of(frontendOrigin));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    /**
     * Rejects the OAuth2 login outright if the Google account's email isn't
     * on the institute domain, before Spring Security ever creates a
     * session for it.
     */
    private DefaultOAuth2UserService domainRestrictedUserService() {
        DefaultOAuth2UserService delegate = new DefaultOAuth2UserService();
        return new DefaultOAuth2UserService() {
            @Override
            public OAuth2User loadUser(OAuth2UserRequest request) {
                OAuth2User user = delegate.loadUser(request);
                String email = user.getAttribute("email");
                if (email == null || !emailDomainPolicy.isAllowed(email)) {
                    throw new OAuth2AuthenticationException(
                            "Only @" + allowedEmailDomain + " accounts may sign in");
                }
                return user;
            }
        };
    }

    @Value("${prayas.tour.rate-limit-per-hour:5}")
    private int rateLimitPerHour;

    @Bean
    public PublicSubmitRateLimitFilter publicSubmitRateLimitFilter() {
        return new PublicSubmitRateLimitFilter(rateLimitPerHour);
    }
}