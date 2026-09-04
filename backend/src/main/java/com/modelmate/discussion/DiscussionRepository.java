package com.modelmate.discussion;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface DiscussionRepository extends JpaRepository<Discussion, Long> {

    @Query("select distinct t from Discussion d join d.tags t order by t")
    List<String> findDistinctTags();
}
