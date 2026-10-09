package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.LoginChallenge;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface LoginChallengeRepository
        extends JpaRepository<LoginChallenge, UUID> {

    Optional<LoginChallenge> findTopByEmployeeIdOrderByCreatedOnDesc(UUID employeeId);

    @Modifying
    @Transactional
    @Query("delete from LoginChallenge c where c.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") UUID employeeId);

    // Atomically uses up one attempt. Returns 0 when no attempts are left.

    @Modifying
    @Transactional
    @Query("update LoginChallenge c set c.attempts = c.attempts + 1 " +
            "where c.id = :id and c.used = false and c.attempts < :max")
    int reserveAttempt(@Param("id") UUID id, @Param("max") int max);

    // Atomically marks the challenge used. Returns 0 if already used.
    @Modifying
    @Transactional
    @Query("update LoginChallenge c set c.used = true " +
            "where c.id = :id and c.used = false")
    int markUsed(@Param("id") UUID id);
}