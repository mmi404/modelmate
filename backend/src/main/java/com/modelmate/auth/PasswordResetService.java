package com.modelmate.auth;

import com.modelmate.auth.event.PasswordResetCodeIssued;
import com.modelmate.security.JwtService;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetService {

    private static final int MAX_ATTEMPTS = 5;
    private static final int EXPIRY_MINUTES = 15;

    private final UserRepository users;
    private final PasswordResetTokenRepository tokens;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final ApplicationEventPublisher events;
    private final SecureRandom random = new SecureRandom();

    /** Always succeeds silently — never reveals whether the email is registered. */
    @Transactional
    public void requestReset(String email) {
        var maybeUser = users.findByEmailIgnoreCase(email.trim());
        if (maybeUser.isEmpty()) {
            return;
        }
        User user = maybeUser.get();
        tokens.deleteByUserId(user.getId());

        String code = String.format("%06d", random.nextInt(1_000_000));
        PasswordResetToken token = new PasswordResetToken();
        token.setUser(user);
        token.setCodeHash(passwordEncoder.encode(code));
        token.setExpiresAt(Instant.now().plus(EXPIRY_MINUTES, ChronoUnit.MINUTES));
        tokens.saveAndFlush(token);

        events.publishEvent(new PasswordResetCodeIssued(user.getEmail(), code));
    }

    @Transactional
    public String verifyCode(String email, String code) {
        User user = users.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));
        PasswordResetToken token = tokens
                .findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(user.getId())
                .orElseThrow(() -> new IllegalArgumentException("Invalid or expired code"));

        if (token.isExpired()) {
            throw new IllegalArgumentException("Invalid or expired code");
        }
        if (token.getAttemptCount() >= MAX_ATTEMPTS) {
            throw new IllegalArgumentException("Too many attempts, request a new code");
        }
        if (!passwordEncoder.matches(code, token.getCodeHash())) {
            token.setAttemptCount(token.getAttemptCount() + 1);
            throw new IllegalArgumentException("Invalid or expired code");
        }
        return jwtService.generateResetTicket(user.getId());
    }

    @Transactional
    public void resetPassword(String resetTicket, String newPassword) {
        Claims claims;
        try {
            claims = jwtService.parse(resetTicket).getPayload();
        } catch (JwtException | IllegalArgumentException ex) {
            throw new IllegalArgumentException("Invalid or expired reset ticket");
        }
        if (!JwtService.PURPOSE_PASSWORD_RESET.equals(claims.get("purpose"))) {
            throw new IllegalArgumentException("Invalid reset ticket");
        }
        Long userId = Long.valueOf(claims.getSubject());
        User user = users.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("Invalid reset ticket"));
        user.setPasswordHash(passwordEncoder.encode(newPassword));

        tokens.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(userId)
                .ifPresent(t -> t.setUsed(true));
        log.info("Password reset completed for user {}", userId);
    }
}
