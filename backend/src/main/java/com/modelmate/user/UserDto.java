package com.modelmate.user;

import java.time.Instant;

public record UserDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        Role role,
        String avatarUrl,
        String bio,
        Instant createdAt
) {
    public static UserDto from(User u) {
        return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), u.getEmail(),
                u.getRole(), u.getAvatarUrl(), u.getBio(), u.getCreatedAt());
    }

    /** Public projection: no email. */
    public static UserDto publicProfile(User u) {
        return new UserDto(u.getId(), u.getFirstName(), u.getLastName(), null,
                u.getRole(), u.getAvatarUrl(), u.getBio(), u.getCreatedAt());
    }
}
