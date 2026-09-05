package com.modelmate.review;

import com.modelmate.common.PageResponse;
import com.modelmate.review.dto.ReviewDtos.CreateReviewRequest;
import com.modelmate.review.dto.ReviewDtos.ReviewDto;
import com.modelmate.review.dto.ReviewDtos.UpdateReviewRequest;
import com.modelmate.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@Tag(name = "Reviews")
public class ReviewController {

    private final ReviewService reviewService;

    @GetMapping("/api/v1/reviews/recent")
    @Operation(summary = "Latest reviews and problem reports across all models (home feed)")
    public java.util.List<com.modelmate.review.dto.ReviewDtos.RecentReviewDto> recent(
            @org.springframework.web.bind.annotation.RequestParam(defaultValue = "8") int limit) {
        return reviewService.recent(limit);
    }

    @GetMapping("/api/v1/models/{modelId}/reviews")
    @Operation(summary = "List visible reviews for a model")
    public PageResponse<ReviewDto> reviews(@PathVariable Long modelId,
                                           @AuthenticationPrincipal AuthUser principal,
                                           @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.listReviews(modelId, principal, pageable);
    }

    @GetMapping("/api/v1/models/{modelId}/problems")
    @Operation(summary = "List visible problem reports for a model")
    public PageResponse<ReviewDto> problems(@PathVariable Long modelId,
                                            @AuthenticationPrincipal AuthUser principal,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return reviewService.listProblems(modelId, principal, pageable);
    }

    @PostMapping("/api/v1/models/{modelId}/reviews")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Post a review or problem report for a model")
    public ReviewDto create(@PathVariable Long modelId,
                            @Valid @RequestBody CreateReviewRequest request,
                            @AuthenticationPrincipal AuthUser principal) {
        return reviewService.create(modelId, request, principal);
    }

    @PutMapping("/api/v1/reviews/{id}")
    @Operation(summary = "Edit your own review or problem report")
    public ReviewDto update(@PathVariable Long id,
                            @Valid @RequestBody UpdateReviewRequest request,
                            @AuthenticationPrincipal AuthUser principal) {
        return reviewService.update(id, request, principal);
    }

    @DeleteMapping("/api/v1/reviews/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete your own review, or any review as an admin")
    public void delete(@PathVariable Long id, @AuthenticationPrincipal AuthUser principal) {
        reviewService.delete(id, principal);
    }
}
