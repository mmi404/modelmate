package com.modelmate.auth;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records a failed reset-code attempt in its own transaction.
 *
 * <p>{@link PasswordResetService#verifyCode} rejects a bad code by throwing, which
 * rolls back its transaction — so incrementing the counter inline silently
 * discarded it and the 5-attempt cap never engaged. REQUIRES_NEW commits the
 * increment independently of that rollback.
 */
@Component
@RequiredArgsConstructor
public class PasswordResetAttemptRecorder {

    private final PasswordResetTokenRepository tokens;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void recordFailedAttempt(Long tokenId) {
        tokens.incrementAttemptCount(tokenId);
    }
}
