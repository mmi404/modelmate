package com.modelmate.auth;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.modelmate.AbstractIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Regression cover for the reset-code brute-force cap.
 *
 * <p>The cap used to be dead code: {@code verifyCode} incremented the counter and
 * then threw, so the surrounding transaction rolled the increment back and every
 * guess was "attempt 1". The original test only asserted a 400 on a wrong code, so
 * it passed throughout. This test asserts the *effect* of the cap instead.
 */
class PasswordResetLockoutTest extends AbstractIntegrationTest {

    private static final String EMAIL = "lockout@example.com";

    @Autowired
    MockMvc mvc;

    @Autowired
    ObjectMapper json;

    @Autowired
    PasswordResetTokenRepository tokens;

    @Autowired
    com.modelmate.user.UserRepository users;

    @Autowired
    com.modelmate.security.RateLimitingFilter rateLimiter;

    private void requestReset() throws Exception {
        mvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"firstName":"Lock","lastName":"Out","email":"%s","password":"password123"}
                                """.formatted(EMAIL)))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/forgot-password")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\"}"))
                .andExpect(status().isNoContent());
    }

    private void attempt(String code, int expectedStatus) throws Exception {
        // The per-IP limiter would cut this test off at 5 calls to the same path;
        // the lockout under test is the token's own attempt cap, so clear it.
        rateLimiter.clearBuckets();
        mvc.perform(post("/api/v1/auth/verify-reset-code")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + EMAIL + "\",\"code\":\"" + code + "\"}"))
                .andExpect(status().is(expectedStatus));
    }

    @Test
    void wrongCodeAttemptsArePersistedAndEventuallyLockTheToken() throws Exception {
        requestReset();
        String realCode = TestPasswordResetCodeCapture.LAST_CODE.get();
        String wrongCode = realCode.equals("000000") ? "111111" : "000000";

        Long userId = users.findByEmailIgnoreCase(EMAIL).orElseThrow().getId();

        // A single wrong guess must survive the rejection (this is what regressed).
        attempt(wrongCode, 400);
        assertThat(tokens.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(userId).orElseThrow()
                .getAttemptCount()).isEqualTo(1);

        // Exhaust the remaining budget.
        for (int i = 0; i < 4; i++) {
            attempt(wrongCode, 400);
        }
        assertThat(tokens.findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(userId).orElseThrow()
                .getAttemptCount()).isEqualTo(5);

        // Now even the correct code must be refused.
        attempt(realCode, 400);
    }
}
