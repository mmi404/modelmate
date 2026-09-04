package com.modelmate.admin.dto;

import com.modelmate.model.ModelStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.Instant;

public final class AdminDtos {

    private AdminDtos() {
    }

    public record PendingModelDto(
            Long id,
            String name,
            String slug,
            String creator,
            String categoryName,
            String categorySlug,
            String description,
            String websiteUrl,
            String submitterName,
            Instant submittedAt
    ) {
    }

    public record ModerationResultDto(Long id, ModelStatus status, String message) {
    }

    public record RejectModelRequest(@NotBlank @Size(max = 2000) String reason) {
    }

    public record HideReviewRequest(@NotNull Boolean hidden) {
    }

    public record AdminStats(
            long totalUsers,
            long pendingModels,
            long approvedModels,
            long rejectedModels,
            long totalReviews,
            long totalProblems,
            long totalDiscussions
    ) {
    }
}
