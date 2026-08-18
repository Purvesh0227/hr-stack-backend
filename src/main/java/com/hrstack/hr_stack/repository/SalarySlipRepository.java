package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.SalarySlip;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SalarySlipRepository  extends JpaRepository<SalarySlip,Long> {
    Optional<SalarySlip> findByEmpIdAndMonthAndYear(
            String empId,
            Integer month,
            Integer year
    );

    List<SalarySlip> findByMonthAndYear(
            Integer month, Integer year);

    List<SalarySlip> findByEmpId(
      String empId
    );
}
