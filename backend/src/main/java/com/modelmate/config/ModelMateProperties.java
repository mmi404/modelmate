package com.modelmate.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * Typed view of the {@code modelmate.*} configuration tree.
 */
@ConfigurationProperties(prefix = "modelmate")
public record ModelMateProperties(
        Cors cors,
        Jwt jwt,
        Mail mail,
        Security security
) {

    public record Cors(@DefaultValue("http://localhost:3000") String allowedOrigins) {
    }

    public record Jwt(
            String secret,
            @DefaultValue("1440") long ttlMinutes,
            @DefaultValue("10") long resetTicketTtlMinutes
    ) {
    }

    public record Mail(@DefaultValue("no-reply@modelmate.local") String from,
                       @DefaultValue("true") boolean enabled) {
    }

    public record Security(Captcha captcha) {
        public record Captcha(@DefaultValue("false") boolean enabled,
                              @DefaultValue("") String turnstileSecret) {
        }
    }
}
