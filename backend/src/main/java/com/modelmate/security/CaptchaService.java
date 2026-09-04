package com.modelmate.security;

import com.modelmate.config.ModelMateProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

/**
 * Verifies Cloudflare Turnstile tokens. No-op unless
 * {@code modelmate.security.captcha.enabled=true}.
 */
@Service
@Slf4j
public class CaptchaService {

    private static final String VERIFY_URL = "https://challenges.cloudflare.com/turnstile/v0/siteverify";

    private final boolean enabled;
    private final String secret;
    private final RestClient restClient;

    public CaptchaService(ModelMateProperties props, RestClient.Builder restClientBuilder) {
        this.enabled = props.security().captcha().enabled();
        this.secret = props.security().captcha().turnstileSecret();
        this.restClient = restClientBuilder.build();
    }

    public void verifyOrThrow(String token, String remoteIp) {
        if (!enabled) {
            return;
        }
        if (token == null || token.isBlank()) {
            throw new IllegalArgumentException("Captcha token is required");
        }
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("secret", secret);
        form.add("response", token);
        if (remoteIp != null) {
            form.add("remoteip", remoteIp);
        }
        try {
            TurnstileResponse response = restClient.post()
                    .uri(VERIFY_URL)
                    .body(form)
                    .retrieve()
                    .body(TurnstileResponse.class);
            if (response == null || !response.success()) {
                throw new IllegalArgumentException("Captcha verification failed");
            }
        } catch (IllegalArgumentException ex) {
            throw ex;
        } catch (Exception ex) {
            log.error("Turnstile verification error: {}", ex.getMessage());
            throw new IllegalArgumentException("Captcha verification failed");
        }
    }

    private record TurnstileResponse(boolean success) {
    }
}
