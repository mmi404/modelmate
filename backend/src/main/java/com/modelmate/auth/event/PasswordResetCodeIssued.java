package com.modelmate.auth.event;

/**
 * Published when a password-reset code is generated. The mail module listens and
 * delivers it (or logs it when mail is disabled).
 */
public record PasswordResetCodeIssued(String email, String code) {
}
