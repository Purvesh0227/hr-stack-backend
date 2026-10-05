package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.PasswordResetOtp;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

public interface PasswordResetOtpRepository
        extends JpaRepository<PasswordResetOtp, UUID> {

    Optional<PasswordResetOtp> findTopByEmployeeIdOrderByCreatedOnDesc(UUID employeeId);

    @Modifying
    @Transactional
    @Query("delete from PasswordResetOtp p where p.employeeId = :employeeId")
    void deleteByEmployeeId(@Param("employeeId") UUID employeeId);
}