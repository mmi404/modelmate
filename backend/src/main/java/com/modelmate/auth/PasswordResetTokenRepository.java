package com.modelmate.auth;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findFirstByUserIdAndUsedFalseOrderByCreatedAtDesc(Long userId);

    void deleteByUserId(Long userId);

    @Modifying(clearAutomatically = true)
    @Query("update PasswordResetToken t set t.attemptCount = t.attemptCount + 1 where t.id = :id")
    void incrementAttemptCount(@Param("id") Long id);
}
