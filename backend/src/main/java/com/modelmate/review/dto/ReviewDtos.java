package com.modelmate.review.dto;

import com.modelmate.review.ReviewType;
import com.modelmate.review.Severity;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;

public final class ReviewDtos {

    private ReviewDtos() {
    }

    public record Ratings(
            @Min(1) @Max(5) Short accuracy,
            @Min(1) @Max(5) Short speed,
            @Min(1) @Max(5) Short cost,
            @Min(1) @Max(5) Short easeOfUse,
            @Min(1) @Max(5) Short reliability
    ) {
        public boolean complete() {
            return accuracy != null && speed != null && cost != null
                    && easeOfUse != null && reliability != null;
        }
    }

    public record ReviewerRef(Long id, String name, String avatarUrl) {
    }

    public record ReviewDto(
            Long id,
            ReviewType type,
            String title,
            String content,
            Ratings ratings,
            BigDecimal overallRating,
            Severity severity,
            ReviewerRef reviewer,
            int upvoteCount,
            int downvoteCount,
            Integer myVote,
            Instant createdAt
    ) {
    }

    public record CreateReviewRequest(
            @NotNull ReviewType type,
            @Size(max = 255) String title,
            @NotBlank @Size(max = 5000) String content,
            @Valid Ratings ratings,
            Severity severity
    ) {
    }

    /** One entry in the home-page "latest reviews" feed. */
    public record RecentReviewDto(
            Long id,
            ReviewType type,
            String title,
            String snippet,
            BigDecimal overallRating,
            Severity severity,
            String modelName,
            String modelSlug,
            String reviewerName,
            Instant createdAt
    ) {
    }

    public record UpdateReviewRequest(
            @Size(max = 255) String title,
            @NotBlank @Size(max = 5000) String content,
            @Valid Ratings ratings,
            Severity severity
    ) {
    }
}
