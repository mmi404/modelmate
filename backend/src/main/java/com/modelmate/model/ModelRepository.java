package com.modelmate.model;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ModelRepository extends JpaRepository<Model, Long> {

    Optional<Model> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatusAndCategoryId(ModelStatus status, Long categoryId);
}
