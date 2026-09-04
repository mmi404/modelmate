package com.modelmate.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class ProfileDtos {

    private ProfileDtos() {
    }

    public enum ContributionType {REVIEW, PROBLEM, DISCUSSION, REPLY}

    public record ContributionDto(
            ContributionType type,
            Long id,
            String title,
            String snippet,
            String modelSlug,
            Long discussionId,
            String severity,
            Instant createdAt
    ) {
    }

    public record UpdateProfileRequest(
            @NotBlank @Size(max = 100) String firstName,
            @NotBlank @Size(max = 100) String lastName,
            @Size(max = 500) String bio,
            @Size(max = 500) String avatarUrl
    ) {
    }
}
