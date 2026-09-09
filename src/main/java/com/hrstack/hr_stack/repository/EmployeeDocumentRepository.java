package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.EmployeeDocument;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface EmployeeDocumentRepository extends JpaRepository<EmployeeDocument, UUID> {
    Optional<EmployeeDocument> findByEmployeeId(UUID employeeId);
}


