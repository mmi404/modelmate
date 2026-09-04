package com.modelmate.vote;

import com.modelmate.common.exception.NotFoundException;
import com.modelmate.discussion.DiscussionRepository;
import com.modelmate.discussion.ReplyRepository;
import com.modelmate.review.ReviewRepository;
import com.modelmate.security.AuthUser;
import com.modelmate.user.UserRepository;
import com.modelmate.vote.dto.VoteDtos.VoteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class VoteService {

    private final VoteRepository votes;
    private final UserRepository users;
    private final DiscussionRepository discussions;
    private final ReplyRepository replies;
    private final ReviewRepository reviews;

    public VoteResult cast(AuthUser principal, VoteTargetType type, Long targetId, int rawValue) {
        short value = (short) Integer.signum(rawValue);
        if (value == 0) {
            throw new IllegalArgumentException("Vote value must be 1 or -1");
        }
        Votable target = loadTarget(type, targetId);

        Vote existing = votes.findByUserIdAndTargetTypeAndTargetId(principal.id(), type, targetId).orElse(null);
        if (existing == null) {
            Vote vote = new Vote();
            vote.setUser(users.getReferenceById(principal.id()));
            vote.setTargetType(type);
            vote.setTargetId(targetId);
            vote.setValue(value);
            votes.save(vote);
            applyDelta(target, 0, value);
        } else if (existing.getValue() != value) {
            applyDelta(target, existing.getValue(), value);
            existing.setValue(value);
        }
        return result(target, (int) value);
    }

    public VoteResult remove(AuthUser principal, VoteTargetType type, Long targetId) {
        Votable target = loadTarget(type, targetId);
        votes.findByUserIdAndTargetTypeAndTargetId(principal.id(), type, targetId).ifPresent(vote -> {
            applyDelta(target, vote.getValue(), (short) 0);
            votes.delete(vote);
        });
        return result(target, null);
    }

    private Votable loadTarget(VoteTargetType type, Long id) {
        return switch (type) {
            case DISCUSSION -> discussions.findById(id).orElseThrow(() -> NotFoundException.of("Discussion", id));
            case REPLY -> replies.findById(id).orElseThrow(() -> NotFoundException.of("Reply", id));
            case REVIEW -> reviews.findById(id).orElseThrow(() -> NotFoundException.of("Review", id));
        };
    }

    /** Move one vote from {@code from} to {@code to} (0 means "none"). */
    private void applyDelta(Votable target, int from, int to) {
        if (from == 1) {
            target.setUpvoteCount(Math.max(0, target.getUpvoteCount() - 1));
        } else if (from == -1) {
            target.setDownvoteCount(Math.max(0, target.getDownvoteCount() - 1));
        }
        if (to == 1) {
            target.setUpvoteCount(target.getUpvoteCount() + 1);
        } else if (to == -1) {
            target.setDownvoteCount(target.getDownvoteCount() + 1);
        }
    }

    private VoteResult result(Votable target, Integer myVote) {
        return new VoteResult(target.getUpvoteCount(), target.getDownvoteCount(), myVote);
    }
}
