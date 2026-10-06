package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.SalarySlip;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;

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

    @Query(value = """
    SELECT s FROM SalarySlip s
    LEFT JOIN Employee e ON e.empId = s.empId
    WHERE (:month = 0 OR s.month = :month)
      AND (:year = 0 OR s.year = :year)
      AND (
            LOWER(s.empId) LIKE :pattern ESCAPE '!'
         OR LOWER(e.email) LIKE :pattern ESCAPE '!'
         OR LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE :pattern ESCAPE '!'
      )
    ORDER BY s.year DESC, s.month DESC, s.id DESC
    """,
            countQuery = """
    SELECT COUNT(s) FROM SalarySlip s
    LEFT JOIN Employee e ON e.empId = s.empId
    WHERE (:month = 0 OR s.month = :month)
      AND (:year = 0 OR s.year = :year)
      AND (
            LOWER(s.empId) LIKE :pattern ESCAPE '!'
         OR LOWER(e.email) LIKE :pattern ESCAPE '!'
         OR LOWER(CONCAT(COALESCE(e.firstName, ''), ' ', COALESCE(e.lastName, ''))) LIKE :pattern ESCAPE '!'
      )
    """)
    Page<SalarySlip> searchAll(
            @Param("month") int month,
            @Param("year") int year,
            @Param("pattern") String pattern,
            Pageable pageable);


    @Query(value = """
    SELECT s FROM SalarySlip s
    WHERE s.empId = :empId
      AND (:month = 0 OR s.month = :month)
      AND (:year = 0 OR s.year = :year)
    ORDER BY s.year DESC, s.month DESC, s.id DESC
    """,
            countQuery = """
    SELECT COUNT(s) FROM SalarySlip s
    WHERE s.empId = :empId
      AND (:month = 0 OR s.month = :month)
      AND (:year = 0 OR s.year = :year)
    """)
    Page<SalarySlip> findMine(
            @Param("empId") String empId,
            @Param("month") int month,
            @Param("year") int year,
            Pageable pageable);
}
