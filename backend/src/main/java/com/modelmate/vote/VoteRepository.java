package com.modelmate.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    Optional<Vote> findByUserIdAndTargetTypeAndTargetId(Long userId, VoteTargetType targetType, Long targetId);

    void deleteByTargetTypeAndTargetId(VoteTargetType targetType, Long targetId);

    @Query("select v from Vote v where v.user.id = :userId and v.targetType = :type and v.targetId in :ids")
    List<Vote> findUserVotes(@Param("userId") Long userId,
                             @Param("type") VoteTargetType type,
                             @Param("ids") Collection<Long> ids);
}
