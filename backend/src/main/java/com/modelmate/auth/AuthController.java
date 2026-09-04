package com.modelmate.auth;

import com.modelmate.auth.dto.AuthDtos.AuthResponse;
import com.modelmate.auth.dto.AuthDtos.ForgotPasswordRequest;
import com.modelmate.auth.dto.AuthDtos.LoginRequest;
import com.modelmate.auth.dto.AuthDtos.RegisterRequest;
import com.modelmate.auth.dto.AuthDtos.ResetPasswordRequest;
import com.modelmate.auth.dto.AuthDtos.ResetTicketResponse;
import com.modelmate.auth.dto.AuthDtos.VerifyResetCodeRequest;
import com.modelmate.security.AuthUser;
import com.modelmate.security.CaptchaService;
import com.modelmate.user.UserDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Auth")
public class AuthController {

    private final AuthService authService;
    private final PasswordResetService passwordResetService;
    private final CaptchaService captchaService;

    @PostMapping("/register")
    @Operation(summary = "Create an account and return an access token")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request, HttpServletRequest http) {
        captchaService.verifyOrThrow(request.captchaToken(), http.getRemoteAddr());
        return authService.register(request);
    }

    @PostMapping("/login")
    @Operation(summary = "Authenticate and return an access token")
    public AuthResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest http) {
        captchaService.verifyOrThrow(request.captchaToken(), http.getRemoteAddr());
        return authService.login(request);
    }

    @GetMapping("/me")
    @Operation(summary = "Return the currently authenticated user")
    public UserDto me(@AuthenticationPrincipal AuthUser principal) {
        return authService.currentUser(principal);
    }

    @PostMapping("/logout")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "No-op for stateless clients; the caller drops the token")
    public void logout() {
        // stateless: nothing to invalidate server-side
    }

    @PostMapping("/forgot-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Send a password-reset code to the email if it is registered")
    public void forgotPassword(@Valid @RequestBody ForgotPasswordRequest request, HttpServletRequest http) {
        captchaService.verifyOrThrow(request.captchaToken(), http.getRemoteAddr());
        passwordResetService.requestReset(request.email());
    }

    @PostMapping("/verify-reset-code")
    @Operation(summary = "Exchange a valid reset code for a short-lived reset ticket")
    public ResetTicketResponse verifyResetCode(@Valid @RequestBody VerifyResetCodeRequest request) {
        return new ResetTicketResponse(passwordResetService.verifyCode(request.email(), request.code()));
    }

    @PostMapping("/reset-password")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Set a new password using a reset ticket")
    public void resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        passwordResetService.resetPassword(request.resetTicket(), request.newPassword());
    }
}
