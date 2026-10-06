package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Employee;
import org.springframework.data.domain.Page;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface EmployeeRepository extends JpaRepository<Employee, UUID> {

    Optional<Employee> findByEmail(String email);

    Optional<Employee> findByEmailIgnoreCase(String email);

    boolean existsByEmailIgnoreCase(String email);

    List<Employee> findByRoleIgnoreCase(String role);

    Page<Employee> findByRoleIgnoreCaseAndEmpIdContainingIgnoreCase(
            String role,
            String empId,
            Pageable pageable
    );

    Optional<Employee> findByEmpId(String empId);

    List<Employee> findByStatusIgnoreCase(String status);

    @Query("""
    SELECT e FROM Employee e
    WHERE LOWER(e.role) = LOWER(:role)
      AND (:status = '' OR e.status = :status)
      AND e.createdOn BETWEEN :from AND :to
      AND (
            LOWER(e.empId) LIKE :pattern ESCAPE '!'
         OR LOWER(e.email) LIKE :pattern ESCAPE '!'
         OR LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE :pattern ESCAPE '!'
      )
    """)
    Page<Employee> searchByRole(
            @Param("role") String role,
            @Param("status") String status,
            @Param("from") long from,
            @Param("to") long to,
            @Param("pattern") String pattern,
            Pageable pageable);

}