package com.modelmate.discussion;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    String ORDER_BY = """
            order by
              case when :sort = 'top'    then (d.upvote_count - d.downvote_count) end desc nulls last,
              case when :sort = 'active' then d.updated_at end desc nulls last,
              d.created_at desc
            """;

    interface TagCount {
        String getTag();

        long getCount();
    }

    @Query("select t as tag, count(t) as count from Discussion d join d.tags t "
            + "group by t order by count(t) desc, t asc")
    List<TagCount> tagCounts();

    @Query(value = "select d.* from discussions d " + ORDER_BY,
            countQuery = "select count(*) from discussions",
            nativeQuery = true)
    Page<Discussion> searchAll(@Param("sort") String sort, Pageable pageable);

    @Query(value = """
            select d.* from discussions d
            where exists (select 1 from discussion_tags dt
                          where dt.discussion_id = d.id and dt.tag in (:tags))
            """ + ORDER_BY,
            countQuery = """
            select count(distinct d.id) from discussions d
            join discussion_tags dt on dt.discussion_id = d.id
            where dt.tag in (:tags)
            """,
            nativeQuery = true)
    Page<Discussion> searchByTags(@Param("tags") Collection<String> tags,
                                  @Param("sort") String sort,
                                  Pageable pageable);

    @Query(value = "select count(distinct author_id) from ("
            + "select author_id from discussions union select author_id from replies) contributors",
            nativeQuery = true)
    long countActiveMembers();
}
