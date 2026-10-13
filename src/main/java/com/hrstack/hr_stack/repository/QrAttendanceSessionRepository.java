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

    // One active QR per department: creating a new one kills the old one
    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE QrAttendanceSession s
               SET s.status = 'REVOKED'
             WHERE s.department = :department
               AND s.status = 'ACTIVE'
            """)
    int revokeActiveByDepartment(@Param("department") String department);


    @Modifying(clearAutomatically = true)
    @Query("""
            UPDATE QrAttendanceSession s
               SET s.emailedOn = :now
             WHERE s.uuid = :id
               AND s.emailedOn IS NULL
            """)
    int markEmailed(@Param("id") UUID id, @Param("now") long now);
}
