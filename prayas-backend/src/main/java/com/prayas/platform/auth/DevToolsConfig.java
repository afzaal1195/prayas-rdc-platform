package com.prayas.platform.auth;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

/**
 * LOCAL PROFILE ONLY. DevActAsFilter has to run *inside* Spring Security's
 * filter chain (after the session's login has been loaded, before
 * permissions are checked), where SecurityConfig adds it. Without this,
 * Spring Boot would also register it as an ordinary servlet filter that
 * runs outside the security chain, where there is no login to swap yet.
 */
@Configuration
@Profile("local")
public class DevToolsConfig {

    @Bean
    public FilterRegistrationBean<DevActAsFilter> devActAsFilterRegistration(DevActAsFilter filter) {
        FilterRegistrationBean<DevActAsFilter> registration = new FilterRegistrationBean<>(filter);
        registration.setEnabled(false);
        return registration;
    }
}
