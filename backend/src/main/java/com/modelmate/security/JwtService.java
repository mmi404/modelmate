package com.modelmate.security;

import com.modelmate.config.ModelMateProperties;
import com.modelmate.user.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jws;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;

@Service
public class JwtService {

    public static final String PURPOSE_PASSWORD_RESET = "PASSWORD_RESET";

    private final SecretKey key;
    private final long accessTtlMinutes;
    private final long resetTtlMinutes;

    /** The secret shipped in application-dev.yml; must never reach a real environment. */
    static final String DEV_SECRET = "dev-only-secret-change-me-at-least-thirty-two-bytes-long!!";

    private static final int MIN_SECRET_BYTES = 32;

    public JwtService(ModelMateProperties props, Environment environment) {
        String secret = props.jwt().secret();
        validateSecret(secret, environment);
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTtlMinutes = props.jwt().ttlMinutes();
        this.resetTtlMinutes = props.jwt().resetTicketTtlMinutes();
    }

    /**
     * Refuses to start on a missing, too-short, or well-known signing secret.
     * Without this a deployment that forgets JWT_SECRET would boot happily and
     * sign tokens with a secret that is public in this repository - anyone could
     * mint themselves an ADMIN token.
     */
    private static void validateSecret(String secret, Environment environment) {
        boolean devOrTest = Arrays.stream(environment.getActiveProfiles())
                .anyMatch(p -> p.equals("dev") || p.equals("test"));

        if (secret == null || secret.isBlank()) {
            throw new IllegalStateException(
                    "JWT_SECRET is not set. Set it to a random value of at least "
                            + MIN_SECRET_BYTES + " bytes before starting the application.");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < MIN_SECRET_BYTES) {
            throw new IllegalStateException(
                    "JWT_SECRET is too short (" + secret.getBytes(StandardCharsets.UTF_8).length
                            + " bytes); HS256 requires at least " + MIN_SECRET_BYTES + ".");
        }
        if (!devOrTest && DEV_SECRET.equals(secret)) {
            throw new IllegalStateException(
                    "JWT_SECRET is still the development placeholder, which is public in this "
                            + "repository. Generate a real secret before deploying.");
        }
    }

    public String generateAccessToken(User user) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(accessTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public String generateResetTicket(Long userId) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(userId.toString())
                .claim("purpose", PURPOSE_PASSWORD_RESET)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(resetTtlMinutes, ChronoUnit.MINUTES)))
                .signWith(key)
                .compact();
    }

    public Jws<Claims> parse(String token) {
        return Jwts.parser().verifyWith(key).build().parseSignedClaims(token);
    }
}
