package com.modelmate.vote;

import com.modelmate.security.AuthUser;
import com.modelmate.vote.dto.VoteDtos.RemoveVoteRequest;
import com.modelmate.vote.dto.VoteDtos.VoteRequest;
import com.modelmate.vote.dto.VoteDtos.VoteResult;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
@Tag(name = "Votes")
public class VoteController {

    private final VoteService voteService;

    @PutMapping
    @Operation(summary = "Cast or change a vote on a discussion, reply or review")
    public VoteResult cast(@Valid @RequestBody VoteRequest request,
                           @AuthenticationPrincipal AuthUser principal) {
        return voteService.cast(principal, request.targetType(), request.targetId(), request.value());
    }

    @DeleteMapping
    @Operation(summary = "Remove your vote from a target")
    public VoteResult remove(@Valid @RequestBody RemoveVoteRequest request,
                             @AuthenticationPrincipal AuthUser principal) {
        return voteService.remove(principal, request.targetType(), request.targetId());
    }
}
