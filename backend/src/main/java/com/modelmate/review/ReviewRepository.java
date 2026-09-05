package com.modelmate.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByModelIdAndUserIdAndType(Long modelId, Long userId, ReviewType type);

    long countByModelIdAndTypeAndStatus(Long modelId, ReviewType type, ReviewStatus status);

    long countByType(ReviewType type);

    Page<Review> findByModelIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long modelId, ReviewType type, ReviewStatus status, Pageable pageable);

    List<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    List<Review> findByUserIdAndStatusOrderByCreatedAtDesc(Long userId, ReviewStatus status, Pageable pageable);

    @Query("""
            select r.model.id as modelId,
                   avg(r.overallRating) as overall,
                   count(r) as reviewCount,
                   avg(r.accuracy) as accuracy,
                   avg(r.speed) as speed,
                   avg(r.cost) as cost,
                   avg(r.easeOfUse) as easeOfUse,
                   avg(r.reliability) as reliability
            from Review r
            where r.type = com.modelmate.review.ReviewType.REVIEW
              and r.status = com.modelmate.review.ReviewStatus.VISIBLE
              and r.model.id in :modelIds
            group by r.model.id
            """)
    List<RatingAggregate> aggregateForModels(@Param("modelIds") Collection<Long> modelIds);

    /**
     * Atomic counter adjustment. Read-modify-write on the entity loses updates when
     * two people vote concurrently; letting the database do the arithmetic does not.
     */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update Review r set r.upvoteCount = r.upvoteCount + :upDelta, "
            + "r.downvoteCount = r.downvoteCount + :downDelta where r.id = :id")
    void adjustVoteCounts(@Param("id") Long id, @Param("upDelta") int upDelta, @Param("downDelta") int downDelta);
}
