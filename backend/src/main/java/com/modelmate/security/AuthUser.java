package com.modelmate.security;

import com.modelmate.user.Role;

/**
 * Authenticated principal stored in the security context.
 * Injected into controllers with {@code @AuthenticationPrincipal AuthUser}.
 */
public record AuthUser(Long id, String email, Role role) {

    public boolean isAdmin() {
        return role == Role.ADMIN;
    }
}
