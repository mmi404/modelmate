package com.modelmate.user;

import com.modelmate.common.PageResponse;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.discussion.Discussion;
import com.modelmate.discussion.DiscussionRepository;
import com.modelmate.discussion.Reply;
import com.modelmate.discussion.ReplyRepository;
import com.modelmate.review.Review;
import com.modelmate.review.ReviewRepository;
import com.modelmate.review.ReviewStatus;
import com.modelmate.review.ReviewType;
import com.modelmate.security.AuthUser;
import com.modelmate.user.dto.ProfileDtos.ContributionDto;
import com.modelmate.user.dto.ProfileDtos.ContributionType;
import com.modelmate.user.dto.ProfileDtos.UpdateProfileRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProfileService {

    private static final int FETCH_CAP = 200;

    private final UserRepository users;
    private final ReviewRepository reviews;
    private final DiscussionRepository discussions;
    private final ReplyRepository replies;

    public UserDto publicProfile(Long userId) {
        return users.findById(userId).map(UserDto::publicProfile)
                .orElseThrow(() -> NotFoundException.of("User", userId));
    }

    @Transactional
    public UserDto updateMe(AuthUser principal, UpdateProfileRequest request) {
        User user = users.findById(principal.id())
                .orElseThrow(() -> NotFoundException.of("User", principal.id()));
        user.setFirstName(request.firstName().trim());
        user.setLastName(request.lastName().trim());
        user.setBio(blankToNull(request.bio()));
        user.setAvatarUrl(blankToNull(request.avatarUrl()));
        return UserDto.from(user);
    }

    public PageResponse<ContributionDto> contributions(Long userId, boolean includeHidden, Pageable pageable) {
        if (!users.existsById(userId)) {
            throw NotFoundException.of("User", userId);
        }
        Pageable cap = PageRequest.of(0, FETCH_CAP);

        List<ContributionDto> all = new ArrayList<>();
        List<Review> reviewRows = includeHidden
                ? reviews.findByUserIdOrderByCreatedAtDesc(userId, cap)
                : reviews.findByUserIdAndStatusOrderByCreatedAtDesc(userId, ReviewStatus.VISIBLE, cap);
        for (Review r : reviewRows) {
            all.add(fromReview(r));
        }
        for (Discussion d : discussions.findByAuthorIdOrderByCreatedAtDesc(userId, cap)) {
            all.add(new ContributionDto(ContributionType.DISCUSSION, d.getId(), d.getTitle(),
                    snippet(d.getContent()), null, d.getId(), null, d.getCreatedAt()));
        }
        for (Reply reply : replies.findByAuthorIdOrderByCreatedAtDesc(userId, cap)) {
            all.add(new ContributionDto(ContributionType.REPLY, reply.getId(), "Reply",
                    snippet(reply.getContent()), null, reply.getDiscussion().getId(), null,
                    reply.getCreatedAt()));
        }
        all.sort(Comparator.comparing(ContributionDto::createdAt).reversed());

        int from = (int) pageable.getOffset();
        int size = pageable.getPageSize();
        List<ContributionDto> content = from >= all.size()
                ? List.of()
                : all.subList(from, Math.min(from + size, all.size()));
        int totalPages = (int) Math.ceil((double) all.size() / size);
        return new PageResponse<>(content, pageable.getPageNumber(), size, all.size(), totalPages);
    }

    private ContributionDto fromReview(Review r) {
        boolean problem = r.getType() == ReviewType.PROBLEM;
        String title = r.getTitle() != null ? r.getTitle()
                : (problem ? "Problem report" : "Review of " + r.getModel().getName());
        return new ContributionDto(
                problem ? ContributionType.PROBLEM : ContributionType.REVIEW,
                r.getId(), title, snippet(r.getContent()),
                r.getModel().getSlug(), null,
                r.getSeverity() == null ? null : r.getSeverity().name(),
                r.getCreatedAt());
    }

    private static String snippet(String text) {
        if (text == null) {
            return "";
        }
        String trimmed = text.strip();
        return trimmed.length() <= 200 ? trimmed : trimmed.substring(0, 197) + "...";
    }

    private static String blankToNull(String s) {
        return (s == null || s.isBlank()) ? null : s.trim();
    }
}
