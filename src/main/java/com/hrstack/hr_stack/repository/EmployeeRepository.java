package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Employee;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmail(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Employee> findByRoleIgnoreCase(String role);

    Optional<Employee> findByEmpId(String empId);

}