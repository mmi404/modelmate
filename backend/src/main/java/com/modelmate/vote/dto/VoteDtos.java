package com.modelmate.vote.dto;

import com.modelmate.vote.VoteTargetType;
import jakarta.validation.constraints.NotNull;

public final class VoteDtos {

    private VoteDtos() {
    }

    public record VoteRequest(
            @NotNull VoteTargetType targetType,
            @NotNull Long targetId,
            @NotNull Integer value
    ) {
    }

    public record RemoveVoteRequest(
            @NotNull VoteTargetType targetType,
            @NotNull Long targetId
    ) {
    }

    public record VoteResult(int upvoteCount, int downvoteCount, Integer myVote) {
    }
}
