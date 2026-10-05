package com.prayas.platform.tour;

/** Abstracts the captcha provider so the service layer doesn't depend on one vendor's SDK. */
public interface CaptchaVerifier {
    boolean verify(String token, String remoteIp);
}
