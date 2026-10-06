package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.dto.AttendanceResponse;
import com.hrstack.hr_stack.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByEmpId(String empId);

    boolean existsByEmpIdAndMarkedOnBetween(String empId, Long startOfDay, Long endOfDay);


    Page<Attendance> findByEmpIdAndMarkedOnBetween(
            String empId,
            Long from,
            Long to,
            Pageable pageable
    );


    Page<Attendance> findByEmpIdContainingIgnoreCaseAndMarkedOnBetween(
            String empId,
            Long from,
            Long to,
            Pageable pageable
    );

    @Query(value = """
    SELECT new com.hrstack.hr_stack.dto.AttendanceResponse(
        a.uuid,
        a.empId,
        CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, '')),
        a.markedOn,
        a.status)
    FROM Attendance a
    JOIN Employee e ON e.empId = a.empId
    WHERE a.markedOn BETWEEN :from AND :to
      AND (
            LOWER(a.empId) LIKE :pattern ESCAPE '!'
         OR LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE :pattern ESCAPE '!'
      )
    ORDER BY a.markedOn DESC, a.uuid
    """,
            countQuery = """
    SELECT COUNT(a)
    FROM Attendance a
    JOIN Employee e ON e.empId = a.empId
    WHERE a.markedOn BETWEEN :from AND :to
      AND (
            LOWER(a.empId) LIKE :pattern ESCAPE '!'
         OR LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE :pattern ESCAPE '!'
      )
    """)
    Page<AttendanceResponse> searchAll(
            @Param("from") long from,
            @Param("to") long to,
            @Param("pattern") String pattern,
            Pageable pageable);


    @Query(value = """
    SELECT new com.hrstack.hr_stack.dto.AttendanceResponse(
        a.uuid,
        a.empId,
        CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, '')),
        a.markedOn,
        a.status)
    FROM Attendance a
    JOIN Employee e ON e.empId = a.empId
    WHERE a.empId = :empId
      AND a.markedOn BETWEEN :from AND :to
    ORDER BY a.markedOn DESC, a.uuid
    """,
            countQuery = """
    SELECT COUNT(a)
    FROM Attendance a
    WHERE a.empId = :empId
      AND a.markedOn BETWEEN :from AND :to
    """)
    Page<AttendanceResponse> findMine(
            @Param("empId") String empId,
            @Param("from") long from,
            @Param("to") long to,
            Pageable pageable);
}