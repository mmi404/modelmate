package com.modelmate.discussion;

import com.modelmate.common.PageResponse;
import com.modelmate.common.exception.ForbiddenException;
import com.modelmate.common.exception.NotFoundException;
import com.modelmate.discussion.dto.DiscussionDtos.AuthorRef;
import com.modelmate.discussion.dto.DiscussionDtos.CreateDiscussionRequest;
import com.modelmate.discussion.dto.DiscussionDtos.CreateReplyRequest;
import com.modelmate.discussion.dto.DiscussionDtos.DiscussionDto;
import com.modelmate.discussion.dto.DiscussionDtos.DiscussionStats;
import com.modelmate.discussion.dto.DiscussionDtos.ReplyDto;
import com.modelmate.discussion.dto.DiscussionDtos.TagCountDto;
import com.modelmate.security.AuthUser;
import com.modelmate.user.User;
import com.modelmate.user.UserRepository;
import com.modelmate.vote.Vote;
import com.modelmate.vote.VoteRepository;
import com.modelmate.vote.VoteTargetType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DiscussionService {

    private static final Set<String> SORTS = Set.of("newest", "active", "top");

    private final DiscussionRepository discussions;
    private final ReplyRepository replies;
    private final UserRepository users;
    private final VoteRepository votes;

    // ----- discussions --------------------------------------------------

    public PageResponse<DiscussionDto> list(Collection<String> tags, String sort, AuthUser principal, Pageable pageable) {
        Set<String> normalized = normalizeTags(tags);
        String sortKey = normalizeSort(sort);
        Pageable request = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        Page<Discussion> page = normalized.isEmpty()
                ? discussions.searchAll(sortKey, request)
                : discussions.searchByTags(normalized, sortKey, request);
        Map<Long, Integer> myVotes = myVotes(principal, VoteTargetType.DISCUSSION,
                page.getContent().stream().map(Discussion::getId).toList());
        return PageResponse.of(page.map(d -> toDto(d, myVotes.get(d.getId()))));
    }

    public DiscussionDto get(Long id, AuthUser principal) {
        Discussion d = discussions.findById(id).orElseThrow(() -> NotFoundException.of("Discussion", id));
        Integer myVote = myVotes(principal, VoteTargetType.DISCUSSION, List.of(id)).get(id);
        return toDto(d, myVote);
    }

    @Transactional
    public DiscussionDto create(CreateDiscussionRequest request, AuthUser principal) {
        Discussion d = new Discussion();
        d.setTitle(request.title().trim());
        d.setContent(request.content().trim());
        d.setAuthor(users.getReferenceById(principal.id()));
        d.setTags(normalizeTags(request.tags()));
        discussions.save(d);
        return toDto(d, null);
    }

    public List<TagCountDto> tagCounts() {
        return discussions.tagCounts().stream()
                .map(t -> new TagCountDto(t.getTag(), t.getCount()))
                .toList();
    }

    public DiscussionStats stats() {
        return new DiscussionStats(discussions.countActiveMembers(),
                discussions.count(), replies.count());
    }

    // ----- replies ------------------------------------------------------

    public List<ReplyDto> replies(Long discussionId, AuthUser principal) {
        if (!discussions.existsById(discussionId)) {
            throw NotFoundException.of("Discussion", discussionId);
        }
        List<Reply> all = replies.findByDiscussionIdOrderByCreatedAtAsc(discussionId);
        Map<Long, Integer> myVotes = myVotes(principal, VoteTargetType.REPLY,
                all.stream().map(Reply::getId).toList());
        return all.stream().map(r -> toDto(r, myVotes.get(r.getId()))).toList();
    }

    @Transactional
    public ReplyDto addReply(Long discussionId, CreateReplyRequest request, AuthUser principal) {
        Discussion discussion = discussions.findById(discussionId)
                .orElseThrow(() -> NotFoundException.of("Discussion", discussionId));

        Reply parent = null;
        if (request.parentReplyId() != null) {
            parent = replies.findById(request.parentReplyId())
                    .orElseThrow(() -> NotFoundException.of("Reply", request.parentReplyId()));
            if (!parent.getDiscussion().getId().equals(discussionId)) {
                throw new ForbiddenException("Parent reply belongs to a different discussion");
            }
            if (parent.getParentReply() != null) {
                parent = parent.getParentReply(); // flatten to one level of threading
            }
        }

        Reply reply = new Reply();
        reply.setDiscussion(discussion);
        reply.setParentReply(parent);
        reply.setAuthor(users.getReferenceById(principal.id()));
        reply.setContent(request.content().trim());
        replies.save(reply);

        discussion.setReplyCount(discussion.getReplyCount() + 1);
        return toDto(reply, null);
    }

    // ----- helpers ----------------------------------------------------

    private Map<Long, Integer> myVotes(AuthUser principal, VoteTargetType type, List<Long> ids) {
        if (principal == null || ids.isEmpty()) {
            return Map.of();
        }
        return votes.findUserVotes(principal.id(), type, ids).stream()
                .collect(Collectors.toMap(Vote::getTargetId, v -> v.getValue().intValue()));
    }

    private static Set<String> normalizeTags(Collection<String> tags) {
        if (tags == null) {
            return new LinkedHashSet<>();
        }
        return tags.stream()
                .filter(t -> t != null && !t.isBlank())
                .map(t -> t.trim().toLowerCase(Locale.ENGLISH))
                .limit(5)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String normalizeSort(String sort) {
        return (sort != null && SORTS.contains(sort.toLowerCase(Locale.ENGLISH)))
                ? sort.toLowerCase(Locale.ENGLISH) : "newest";
    }

    private DiscussionDto toDto(Discussion d, Integer myVote) {
        User a = d.getAuthor();
        return new DiscussionDto(d.getId(), d.getTitle(), d.getContent(),
                List.copyOf(d.getTags()),
                new AuthorRef(a.getId(), a.fullName(), a.getAvatarUrl()),
                d.getReplyCount(), d.getUpvoteCount(), d.getDownvoteCount(), myVote, d.getCreatedAt());
    }

    private ReplyDto toDto(Reply r, Integer myVote) {
        User a = r.getAuthor();
        return new ReplyDto(r.getId(), r.getDiscussion().getId(),
                r.getParentReply() == null ? null : r.getParentReply().getId(),
                new AuthorRef(a.getId(), a.fullName(), a.getAvatarUrl()),
                r.getContent(), r.getUpvoteCount(), r.getDownvoteCount(), myVote, r.getCreatedAt());
    }
}
