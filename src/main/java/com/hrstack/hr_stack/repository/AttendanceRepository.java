package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Attendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByEmpId(String empId);

}
