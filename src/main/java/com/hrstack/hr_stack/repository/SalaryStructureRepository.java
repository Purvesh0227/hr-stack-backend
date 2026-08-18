package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.SalaryStructure;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface SalaryStructureRepository extends JpaRepository<SalaryStructure, Long> {
    Optional<SalaryStructure> findByEmpId(String empId);
}
