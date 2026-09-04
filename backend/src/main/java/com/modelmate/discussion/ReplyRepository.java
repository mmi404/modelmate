package com.modelmate.discussion;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    long countByDiscussionId(Long discussionId);
}
