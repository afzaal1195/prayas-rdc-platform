package com.prayas.platform.auth;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * The one definition of "is this an institute email address?", shared by
 * Google sign-in (SecurityConfig) and the admin screen that creates staff
 * accounts -- so an account can never be created that sign-in would refuse.
 *
 * Accepts the exact domain and any subdomain of it (someone@cse.iith.ac.in
 * for iith.ac.in), but compares only the part after the last '@', so a
 * lookalike such as someone@notiith.ac.in is rejected.
 */
@Component
public class EmailDomainPolicy {

    private final String allowedDomain;

    public EmailDomainPolicy(@Value("${prayas.auth.allowed-email-domain}") String allowedDomain) {
        this.allowedDomain = allowedDomain.toLowerCase();
    }

    public boolean isAllowed(String email) {
        if (email == null) {
            return false;
        }
        int at = email.lastIndexOf('@');
        if (at < 0) {
            return false;
        }
        String domain = email.substring(at + 1).toLowerCase();
        return domain.equals(allowedDomain) || domain.endsWith("." + allowedDomain);
    }

    public String allowedDomain() {
        return allowedDomain;
    }
}
