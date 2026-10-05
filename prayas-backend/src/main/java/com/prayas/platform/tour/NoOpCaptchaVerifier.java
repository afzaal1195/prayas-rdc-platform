package com.prayas.platform.tour;

import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Always passes. Only wired up under the "local" profile (see
 * application-local.yml) so a developer running things on their own
 * machine isn't blocked by a real hCaptcha secret. Never active in
 * "prod" -- HCaptchaVerifier is the one used there.
 */
@Component
@Profile("local")
public class NoOpCaptchaVerifier implements CaptchaVerifier {
    @Override
    public boolean verify(String token, String remoteIp) {
        return true;
    }
}
