package com.modelmate.discussion.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.Instant;
import java.util.List;
import java.util.Set;

public final class DiscussionDtos {

    private DiscussionDtos() {
    }

    public record AuthorRef(Long id, String name, String avatarUrl) {
    }

    public record DiscussionDto(
            Long id,
            String title,
            String content,
            List<String> tags,
            AuthorRef author,
            int replyCount,
            int upvoteCount,
            int downvoteCount,
            Integer myVote,
            Instant createdAt
    ) {
    }

    public record ReplyDto(
            Long id,
            Long discussionId,
            Long parentReplyId,
            AuthorRef author,
            String content,
            int upvoteCount,
            int downvoteCount,
            Integer myVote,
            Instant createdAt
    ) {
    }

    public record CreateDiscussionRequest(
            @NotBlank @Size(max = 500) String title,
            @NotBlank @Size(max = 20000) String content,
            @Size(max = 5) Set<@NotBlank @Size(max = 50) String> tags
    ) {
    }

    public record CreateReplyRequest(
            @NotBlank @Size(max = 10000) String content,
            Long parentReplyId
    ) {
    }

    public record DiscussionStats(long activeMembers, long totalDiscussions, long totalReplies) {
    }

    public record TagCountDto(String tag, long count) {
    }
}
