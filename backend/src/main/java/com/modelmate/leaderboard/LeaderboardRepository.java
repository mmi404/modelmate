package com.modelmate.leaderboard;

import com.modelmate.model.Model;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * Read-model for the leaderboard. Bound to {@link Model} only to satisfy the
 * {@code JpaRepository} type parameter — every method is a projection query.
 */
public interface LeaderboardRepository extends JpaRepository<Model, Long> {

    interface LeaderboardRow {
        Long getModelId();

        String getModelName();

        String getModelSlug();

        String getCreator();

        String getCategorySlug();

        String getCategoryName();

        Double getOverall();

        Long getReviewCount();
    }

    @Query(value = """
            select m.id            as modelId,
                   m.name          as modelName,
                   m.slug          as modelSlug,
                   m.creator       as creator,
                   c.slug          as categorySlug,
                   c.name          as categoryName,
                   agg.avg_rating  as overall,
                   agg.cnt         as reviewCount
            from models m
            join categories c on c.id = m.category_id
            join (
                select model_id, avg(overall_rating) avg_rating, count(*) cnt
                from reviews
                where type = 'REVIEW' and status = 'VISIBLE'
                group by model_id
                having count(*) >= :minReviews
            ) agg on agg.model_id = m.id
            where m.status = 'APPROVED'
              and (:categorySlug is null or c.slug = :categorySlug)
            order by agg.avg_rating desc, agg.cnt desc, m.name asc
            limit 100
            """, nativeQuery = true)
    List<LeaderboardRow> topModels(@Param("categorySlug") String categorySlug,
                                   @Param("minReviews") int minReviews);
}
