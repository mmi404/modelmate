package com.modelmate.review;

import com.modelmate.common.PageResponse;
import com.modelmate.common.exception.ConflictException;
import com.modelmate.common.exception.ForbiddenException;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.model.Model;
import com.modelmate.model.ModelRepository;
import com.modelmate.model.ModelStatus;
import com.modelmate.review.dto.ReviewDtos.CreateReviewRequest;
import com.modelmate.review.dto.ReviewDtos.Ratings;
import com.modelmate.review.dto.ReviewDtos.ReviewDto;
import com.modelmate.review.dto.ReviewDtos.RecentReviewDto;
import com.modelmate.review.dto.ReviewDtos.ReviewerRef;
import com.modelmate.review.dto.ReviewDtos.UpdateReviewRequest;
import com.modelmate.security.AuthUser;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import com.modelmate.vote.Vote;
import com.modelmate.vote.VoteRepository;
import com.modelmate.vote.VoteTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewService {

    private final ReviewRepository reviews;
    private final ModelRepository models;
    private final UserRepository users;
    private final VoteRepository votes;

    public PageResponse<ReviewDto> listReviews(Long modelId, AuthUser principal, Pageable pageable) {
        return list(modelId, ReviewType.REVIEW, principal, pageable);
    }

    public PageResponse<ReviewDto> listProblems(Long modelId, AuthUser principal, Pageable pageable) {
        return list(modelId, ReviewType.PROBLEM, principal, pageable);
    }

    public List<RecentReviewDto> recent(int limit) {
        int capped = Math.min(Math.max(limit, 1), 20);
        return reviews.findRecentVisible(org.springframework.data.domain.PageRequest.of(0, capped)).stream()
                .map(r -> new RecentReviewDto(
                        r.getId(), r.getType(), r.getTitle(), snippet(r.getContent()),
                        r.getOverallRating(), r.getSeverity(),
                        r.getModel().getName(), r.getModel().getSlug(),
                        r.getUser().fullName(), r.getCreatedAt()))
                .toList();
    }

    private static String snippet(String content) {
        String trimmed = content.strip();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 200).strip() + "…";
    }

    private PageResponse<ReviewDto> list(Long modelId, ReviewType type, AuthUser principal, Pageable pageable) {
        requireApprovedModel(modelId);
        Page<Review> page = reviews.findByModelIdAndTypeAndStatusOrderByCreatedAtDesc(
                modelId, type, ReviewStatus.VISIBLE, pageable);
        Map<Long, Integer> myVotes = myVotes(principal, page.getContent());
        return PageResponse.of(page.map(r -> toDto(r, myVotes.get(r.getId()))));
    }

    @Transactional
    public ReviewDto create(Long modelId, CreateReviewRequest request, AuthUser principal) {
        Model model = requireApprovedModel(modelId);
        ReviewType type = request.type();

        if (type == ReviewType.REVIEW) {
            requireCompleteRatings(request.ratings());
            if (reviews.existsByModelIdAndUserIdAndType(modelId, principal.id(), ReviewType.REVIEW)) {
                throw new ConflictException("You have already reviewed this model");
            }
        }

        Review review = new Review();
        review.setModel(model);
        review.setUser(users.getReferenceById(principal.id()));
        review.setType(type);
        review.setTitle(blankToNull(request.title()));
        review.setContent(request.content().trim());
        review.setStatus(ReviewStatus.VISIBLE);
        applyRatings(review, type, request.ratings());
        review.setSeverity(type == ReviewType.PROBLEM
                ? (request.severity() == null ? Severity.MEDIUM : request.severity())
                : null);

        reviews.save(review);
        return toDto(review, null);
    }

    @Transactional
    public ReviewDto update(Long reviewId, UpdateReviewRequest request, AuthUser principal) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        if (!review.getUser().getId().equals(principal.id())) {
            throw new ForbiddenException("You can only edit your own contribution");
        }
        review.setTitle(blankToNull(request.title()));
        review.setContent(request.content().trim());
        if (review.getType() == ReviewType.REVIEW) {
            requireCompleteRatings(request.ratings());
            applyRatings(review, ReviewType.REVIEW, request.ratings());
        } else if (request.severity() != null) {
            review.setSeverity(request.severity());
        }
        return toDto(review, null);
    }

    @Transactional
    public void delete(Long reviewId, AuthUser principal) {
        Review review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        if (!review.getUser().getId().equals(principal.id()) && !principal.isAdmin()) {
            throw new ForbiddenException("You can only delete your own contribution");
        }
        votes.deleteByTargetTypeAndTargetId(VoteTargetType.REVIEW, reviewId);
        reviews.delete(review);
    }

    // ----- helpers ----------------------------------------------------

    private Model requireApprovedModel(Long modelId) {
        return models.findById(modelId)
                .filter(m -> m.getStatus() == ModelStatus.APPROVED)
                .orElseThrow(() -> NotFoundException.of("Model", modelId));
    }

    private static void requireCompleteRatings(Ratings ratings) {
        if (ratings == null || !ratings.complete()) {
            throw new IllegalArgumentException("All five ratings (1-5) are required for a review");
        }
    }

    private static void applyRatings(Review review, ReviewType type, Ratings ratings) {
        if (type != ReviewType.REVIEW || ratings == null) {
            review.setAccuracy(null);
            review.setSpeed(null);
            review.setCost(null);
            review.setEaseOfUse(null);
            review.setReliability(null);
            review.recomputeOverall();
            return;
        }
        review.setAccuracy(ratings.accuracy());
        review.setSpeed(ratings.speed());
        review.setCost(ratings.cost());
        review.setEaseOfUse(ratings.easeOfUse());
        review.setReliability(ratings.reliability());
        review.recomputeOverall();
    }

    private Map<Long, Integer> myVotes(AuthUser principal, List<Review> list) {
        if (principal == null || list.isEmpty()) {
            return Map.of();
        }
        List<Long> ids = list.stream().map(Review::getId).toList();
        return votes.findUserVotes(principal.id(), VoteTargetType.REVIEW, ids).stream()
                .collect(Collectors.toMap(Vote::getTargetId, v -> v.getValue().intValue()));
    }

    private ReviewDto toDto(Review r, Integer myVote) {
        User author = r.getUser();
        Ratings ratings = r.getType() == ReviewType.REVIEW
                ? new Ratings(r.getAccuracy(), r.getSpeed(), r.getCost(), r.getEaseOfUse(), r.getReliability())
                : null;
        return new ReviewDto(r.getId(), r.getType(), r.getTitle(), r.getContent(), ratings,
                r.getOverallRating(), r.getSeverity(),
                new ReviewerRef(author.getId(), author.fullName(), author.getAvatarUrl()),
                r.getUpvoteCount(), r.getDownvoteCount(), myVote, r.getCreatedAt());
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
