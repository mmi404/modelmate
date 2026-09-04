package com.modelmate.discussion;

import com.modelmate.common.PageResponse;
import com.modelmate.discussion.dto.DiscussionDtos.CreateDiscussionRequest;
import com.modelmate.discussion.dto.DiscussionDtos.CreateReplyRequest;
import com.modelmate.discussion.dto.DiscussionDtos.DiscussionDto;
import com.modelmate.discussion.dto.DiscussionDtos.DiscussionStats;
import com.modelmate.discussion.dto.DiscussionDtos.ReplyDto;
import com.modelmate.discussion.dto.DiscussionDtos.TagCountDto;
import com.modelmate.security.AuthUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/discussions")
@RequiredArgsConstructor
@Tag(name = "Discussions")
public class DiscussionController {

    private final DiscussionService discussionService;

    @GetMapping
    @Operation(summary = "List discussions, optionally filtered by tags")
    public PageResponse<DiscussionDto> list(@RequestParam(required = false) List<String> tags,
                                            @RequestParam(required = false) String sort,
                                            @AuthenticationPrincipal AuthUser principal,
                                            @PageableDefault(size = 20) Pageable pageable) {
        return discussionService.list(tags, sort, principal, pageable);
    }

    @GetMapping("/tags")
    @Operation(summary = "Distinct discussion tags with usage counts")
    public List<TagCountDto> tags() {
        return discussionService.tagCounts();
    }

    @GetMapping("/stats")
    @Operation(summary = "Community activity totals")
    public DiscussionStats stats() {
        return discussionService.stats();
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get one discussion")
    public DiscussionDto get(@PathVariable Long id, @AuthenticationPrincipal AuthUser principal) {
        return discussionService.get(id, principal);
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Start a new discussion")
    public DiscussionDto create(@Valid @RequestBody CreateDiscussionRequest request,
                                @AuthenticationPrincipal AuthUser principal) {
        return discussionService.create(request, principal);
    }

    @GetMapping("/{id}/replies")
    @Operation(summary = "List replies for a discussion (one level of threading)")
    public List<ReplyDto> replies(@PathVariable Long id, @AuthenticationPrincipal AuthUser principal) {
        return discussionService.replies(id, principal);
    }

    @PostMapping("/{id}/replies")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Reply to a discussion")
    public ReplyDto reply(@PathVariable Long id,
                          @Valid @RequestBody CreateReplyRequest request,
                          @AuthenticationPrincipal AuthUser principal) {
        return discussionService.addReply(id, request, principal);
    }
}
