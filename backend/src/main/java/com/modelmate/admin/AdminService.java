package com.modelmate.admin;

import com.modelmate.admin.dto.AdminDtos.AdminStats;
import com.modelmate.admin.dto.AdminDtos.ModerationResultDto;
import com.modelmate.admin.dto.AdminDtos.PendingModelDto;
import com.modelmate.common.PageResponse;
import com.modelmate.common.exception.ConflictException;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.discussion.DiscussionRepository;
import com.modelmate.model.Model;
import com.modelmate.model.ModelRepository;
import com.modelmate.model.ModelStatus;
import com.modelmate.review.ReviewRepository;
import com.modelmate.review.ReviewStatus;
import com.modelmate.review.ReviewType;
import com.modelmate.security.AuthUser;
import com.modelmate.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminService {

    private final ModelRepository models;
    private final ReviewRepository reviews;
    private final DiscussionRepository discussions;
    private final UserRepository users;

    public PageResponse<PendingModelDto> pendingModels(Pageable pageable) {
        Page<Model> page = models.findByStatusOrderByCreatedAtAsc(ModelStatus.PENDING, pageable);
        return PageResponse.of(page.map(m -> new PendingModelDto(
                m.getId(), m.getName(), m.getSlug(), m.getCreator(),
                m.getCategory().getName(), m.getCategory().getSlug(),
                m.getDescription(), m.getWebsiteUrl(),
                m.getSubmittedBy().fullName(), m.getCreatedAt())));
    }

    @Transactional
    public ModerationResultDto approve(Long modelId, AuthUser admin) {
        Model model = requirePending(modelId);
        model.setStatus(ModelStatus.APPROVED);
        model.setApprovedAt(Instant.now());
        model.setApprovedBy(users.getReferenceById(admin.id()));
        model.setRejectionReason(null);
        return new ModerationResultDto(model.getId(), ModelStatus.APPROVED, "Model approved and published");
    }

    @Transactional
    public ModerationResultDto reject(Long modelId, String reason) {
        Model model = requirePending(modelId);
        model.setStatus(ModelStatus.REJECTED);
        model.setRejectionReason(reason);
        return new ModerationResultDto(model.getId(), ModelStatus.REJECTED, "Model submission rejected");
    }

    @Transactional
    public void setReviewHidden(Long reviewId, boolean hidden) {
        var review = reviews.findById(reviewId)
                .orElseThrow(() -> NotFoundException.of("Review", reviewId));
        review.setStatus(hidden ? ReviewStatus.HIDDEN : ReviewStatus.VISIBLE);
    }

    public AdminStats stats() {
        return new AdminStats(
                users.count(),
                models.countByStatus(ModelStatus.PENDING),
                models.countByStatus(ModelStatus.APPROVED),
                models.countByStatus(ModelStatus.REJECTED),
                reviews.countByType(ReviewType.REVIEW),
                reviews.countByType(ReviewType.PROBLEM),
                discussions.count());
    }

    private Model requirePending(Long modelId) {
        Model model = models.findById(modelId)
                .orElseThrow(() -> NotFoundException.of("Model", modelId));
        if (model.getStatus() != ModelStatus.PENDING) {
            throw new ConflictException("Model is not pending review (status: " + model.getStatus() + ")");
        }
        return model;
    }
}
