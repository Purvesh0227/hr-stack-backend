package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.EmailVerification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface EmailVerificationRepository
        extends JpaRepository<EmailVerification, UUID> {

    Optional<EmailVerification> findTopByEmailOrderByCreatedOnDesc(String email);

    Optional<EmailVerification> findByTokenHash(String tokenHash);

    @Modifying
    @Transactional
    @Query("delete from EmailVerification v where v.email = :email")
    void deleteByEmail(@Param("email") String email);
}