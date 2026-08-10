package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Otp;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface OtpRepository extends JpaRepository<Otp, UUID> {
    Optional<Otp> findTopByOrderByCreatedOnDesc();
}
