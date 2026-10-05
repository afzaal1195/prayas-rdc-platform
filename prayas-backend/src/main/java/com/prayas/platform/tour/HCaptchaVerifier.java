package com.prayas.platform.tour;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@Profile("!local")
public class HCaptchaVerifier implements CaptchaVerifier {

    private final RestClient restClient = RestClient.create("https://hcaptcha.com");
    private final String secret;

    public HCaptchaVerifier(@Value("${prayas.captcha.secret}") String secret) {
        this.secret = secret;
    }

    @Override
    public boolean verify(String token, String remoteIp) {
        if (token == null || token.isBlank()) {
            return false;
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        form.add("remoteip", remoteIp);

        Map<?, ?> result = restClient.post()
                .uri("/siteverify")
                .body(form)
                .retrieve()
                .body(Map.class);

        return result != null && Boolean.TRUE.equals(result.get("success"));
    }
}
