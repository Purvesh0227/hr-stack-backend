package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Attendance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

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
}