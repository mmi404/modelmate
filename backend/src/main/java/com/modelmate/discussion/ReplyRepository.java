package com.modelmate.discussion;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.jpa.repository.Modifying;

import java.util.List;

public interface ReplyRepository extends JpaRepository<Reply, Long> {

    List<Reply> findByDiscussionIdOrderByCreatedAtAsc(Long discussionId);

    long countByDiscussionId(Long discussionId);

    List<Reply> findByAuthorIdOrderByCreatedAtDesc(Long authorId, Pageable pageable);

    /**
     * Atomic counter adjustment. Read-modify-write on the entity loses updates when
     * two people vote concurrently; letting the database do the arithmetic does not.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Reply r set r.upvoteCount = r.upvoteCount + :upDelta, "
            + "r.downvoteCount = r.downvoteCount + :downDelta where r.id = :id")
    void adjustVoteCounts(@Param("id") Long id, @Param("upDelta") int upDelta, @Param("downDelta") int downDelta);
}
