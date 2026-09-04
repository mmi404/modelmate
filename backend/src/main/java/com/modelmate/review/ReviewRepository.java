package com.modelmate.review;

import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    boolean existsByModelIdAndUserIdAndType(Long modelId, Long userId, ReviewType type);

    long countByModelIdAndTypeAndStatus(Long modelId, ReviewType type, ReviewStatus status);
}
