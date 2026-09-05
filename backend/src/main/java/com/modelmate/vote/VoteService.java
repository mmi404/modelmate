package com.modelmate.vote;

import com.modelmate.common.exception.NotFoundException;
import com.modelmate.discussion.DiscussionRepository;
import com.modelmate.discussion.ReplyRepository;
import com.modelmate.review.ReviewRepository;
import com.modelmate.security.AuthUser;
import com.modelmate.user.UserRepository;
import com.modelmate.vote.dto.VoteDtos.VoteResult;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
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
        requireTargetExists(type, targetId);

        Vote existing = votes.findByUserIdAndTargetTypeAndTargetId(principal.id(), type, targetId).orElse(null);

        if (existing == null) {
            Vote vote = new Vote();
            vote.setUser(users.getReferenceById(principal.id()));
            vote.setTargetType(type);
            vote.setTargetId(targetId);
            vote.setValue(value);
            try {
                votes.saveAndFlush(vote);
            } catch (DataIntegrityViolationException ex) {
                // The same user voted concurrently (e.g. a double-click). The unique
                // constraint did its job; surface it rather than double-counting.
                throw new com.modelmate.common.exception.ConflictException(
                        "You have already voted on this item");
            }
            applyDelta(type, targetId, 0, value);
        } else if (existing.getValue() != value) {
            short previous = existing.getValue();
            existing.setValue(value);
            votes.flush();
            applyDelta(type, targetId, previous, value);
        }

        return currentCounts(type, targetId, (int) value);
    }

    public VoteResult remove(AuthUser principal, VoteTargetType type, Long targetId) {
        requireTargetExists(type, targetId);
        Vote existing = votes.findByUserIdAndTargetTypeAndTargetId(principal.id(), type, targetId).orElse(null);
        if (existing != null) {
            short previous = existing.getValue();
            votes.delete(existing);
            votes.flush();
            applyDelta(type, targetId, previous, (short) 0);
        }
        return currentCounts(type, targetId, null);
    }

    /**
     * Moves one vote from {@code from} to {@code to} (0 meaning "none") using an
     * atomic {@code count = count + delta} statement, so concurrent voters cannot
     * lose each other's increments.
     */
    private void applyDelta(VoteTargetType type, Long targetId, int from, int to) {
        int upDelta = (to == 1 ? 1 : 0) - (from == 1 ? 1 : 0);
        int downDelta = (to == -1 ? 1 : 0) - (from == -1 ? 1 : 0);
        if (upDelta == 0 && downDelta == 0) {
            return;
        }
        switch (type) {
            case DISCUSSION -> discussions.adjustVoteCounts(targetId, upDelta, downDelta);
            case REPLY -> replies.adjustVoteCounts(targetId, upDelta, downDelta);
            case REVIEW -> reviews.adjustVoteCounts(targetId, upDelta, downDelta);
        }
    }

    private void requireTargetExists(VoteTargetType type, Long id) {
        boolean exists = switch (type) {
            case DISCUSSION -> discussions.existsById(id);
            case REPLY -> replies.existsById(id);
            case REVIEW -> reviews.existsById(id);
        };
        if (!exists) {
            throw NotFoundException.of(type.name().charAt(0) + type.name().substring(1).toLowerCase(), id);
        }
    }

    /** Re-reads after the atomic update (the modifying query clears the context). */
    private VoteResult currentCounts(VoteTargetType type, Long targetId, Integer myVote) {
        Votable target = switch (type) {
            case DISCUSSION -> discussions.findById(targetId).orElseThrow(() -> NotFoundException.of("Discussion", targetId));
            case REPLY -> replies.findById(targetId).orElseThrow(() -> NotFoundException.of("Reply", targetId));
            case REVIEW -> reviews.findById(targetId).orElseThrow(() -> NotFoundException.of("Review", targetId));
        };
        return new VoteResult(target.getUpvoteCount(), target.getDownvoteCount(), myVote);
    }
}
