package com.modelmate.model;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ModelRepository extends JpaRepository<Model, Long> {

    Optional<Model> findBySlug(String slug);

    boolean existsBySlug(String slug);

    long countByStatusAndCategoryId(ModelStatus status, Long categoryId);

    interface CategoryCount {
        Long getCategoryId();

        long getCount();
    }

    @Query("select m.category.id as categoryId, count(m) as count from Model m "
            + "where m.status = com.modelmate.model.ModelStatus.APPROVED group by m.category.id")
    List<CategoryCount> countApprovedByCategory();

    List<Model> findByStatusOrderByCreatedAtDesc(ModelStatus status);

    Page<Model> findByStatusOrderByCreatedAtAsc(ModelStatus status, Pageable pageable);

    long countByStatus(ModelStatus status);

    @Query("select m from Model m where m.status = com.modelmate.model.ModelStatus.APPROVED "
            + "and (lower(m.name) like lower(concat('%', :q, '%')) "
            + "or lower(coalesce(m.creator, '')) like lower(concat('%', :q, '%'))) "
            + "order by m.name asc")
    List<Model> searchApprovedByName(@Param("q") String q, Pageable pageable);

    /**
     * Approved models with optional category / text filter, ordered by the requested
     * strategy: {@code rating}, {@code reviews}, {@code name}, otherwise newest first.
     */
    @Query(value = """
            select m.*
            from models m
            join categories c on c.id = m.category_id
            left join (
                select model_id, avg(overall_rating) avg_rating, count(*) cnt
                from reviews
                where type = 'REVIEW' and status = 'VISIBLE'
                group by model_id
            ) agg on agg.model_id = m.id
            where m.status = 'APPROVED'
              and (:categorySlug is null or c.slug = :categorySlug)
              and (:q is null
                   or m.name ilike ('%' || :q || '%')
                   or coalesce(m.creator, '') ilike ('%' || :q || '%'))
            order by
              case when :sort = 'rating'  then coalesce(agg.avg_rating, 0) end desc nulls last,
              case when :sort = 'reviews' then coalesce(agg.cnt, 0) end desc nulls last,
              case when :sort = 'name'    then m.name end asc nulls last,
              m.created_at desc
            """,
            countQuery = """
            select count(*)
            from models m
            join categories c on c.id = m.category_id
            where m.status = 'APPROVED'
              and (:categorySlug is null or c.slug = :categorySlug)
              and (:q is null
                   or m.name ilike ('%' || :q || '%')
                   or coalesce(m.creator, '') ilike ('%' || :q || '%'))
            """,
            nativeQuery = true)
    Page<Model> searchApproved(@Param("categorySlug") String categorySlug,
                               @Param("q") String q,
                               @Param("sort") String sort,
                               Pageable pageable);

    /** Approved models with the most visible reviews created in the trailing window. */
    @Query(value = """
            select m.*
            from models m
            left join (
                select model_id, count(*) recent_cnt
                from reviews
                where type = 'REVIEW' and status = 'VISIBLE'
                  and created_at > now() - interval '30 days'
                group by model_id
            ) recent on recent.model_id = m.id
            where m.status = 'APPROVED'
            order by coalesce(recent.recent_cnt, 0) desc, m.created_at desc
            limit :limit
            """,
            nativeQuery = true)
    List<Model> findTrending(@Param("limit") int limit);
}
