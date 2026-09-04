package com.modelmate.auth;

import com.modelmate.auth.event.PasswordResetCodeIssued;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.event.EventListener;

import java.util.concurrent.atomic.AtomicReference;

/**
 * Captures the most recently issued password-reset code so integration tests can
 * complete the reset flow without reading email.
 */
@TestConfiguration
public class TestPasswordResetCodeCapture {

    public static final AtomicReference<String> LAST_CODE = new AtomicReference<>();

    @EventListener
    public void onCode(PasswordResetCodeIssued event) {
        LAST_CODE.set(event.code());
    }
}
