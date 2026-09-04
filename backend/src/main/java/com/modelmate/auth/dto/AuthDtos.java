package com.modelmate.auth.dto;

import com.modelmate.user.UserDto;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * Request/response payloads for {@code /api/v1/auth}.
 */
public final class AuthDtos {

    private AuthDtos() {
    }

    public record RegisterRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @NotBlank @Email @Size(max = 255) String email,
            @NotBlank @Size(min = 8, max = 100) String password,
            String captchaToken
    ) {
    }

    public record LoginRequest(
            @NotBlank @Email String email,
            @NotBlank String password,
            String captchaToken
    ) {
    }

    public record AuthResponse(String token, UserDto user) {
    }

    public record ForgotPasswordRequest(
            @NotBlank @Email String email,
            String captchaToken
    ) {
    }

    public record VerifyResetCodeRequest(
            @NotBlank @Email String email,
            @NotBlank @Pattern(regexp = "\\d{6}", message = "must be a 6-digit code") String code
    ) {
    }

    public record ResetTicketResponse(String resetTicket) {
    }

    public record ResetPasswordRequest(
            @NotBlank String resetTicket,
            @NotBlank @Size(min = 8, max = 100) String newPassword
    ) {
    }
}
