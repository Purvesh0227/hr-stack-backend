package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.QrAttendanceSession;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface QrAttendanceSessionRepository
        extends JpaRepository<QrAttendanceSession, UUID> {

    Optional<QrAttendanceSession> findByTokenHash(String tokenHash);

//creating new one kills old one
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE QrAttendanceSession s
               SET s.status = 'REVOKED'
             WHERE s.department = :department
               AND s.status = 'ACTIVE'
            """)
    int revokeActiveByDepartment(@Param("department") String department);
}