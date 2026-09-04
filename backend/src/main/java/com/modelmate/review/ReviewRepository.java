package com.modelmate.review;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByModelIdAndUserIdAndType(Long modelId, Long userId, ReviewType type);

    long countByModelIdAndTypeAndStatus(Long modelId, ReviewType type, ReviewStatus status);

    Page<Review> findByModelIdAndTypeAndStatusOrderByCreatedAtDesc(
            Long modelId, ReviewType type, ReviewStatus status, Pageable pageable);

    Page<Review> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

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
}
