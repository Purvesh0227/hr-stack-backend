package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.SalarySlip;
import com.hrstack.hr_stack.entity.SalaryStructure;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.SalarySlipRepository;
import com.hrstack.hr_stack.repository.SalaryStructureRepository;
import com.hrstack.hr_stack.util.SalaryCalculationUtil;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SalaryCalculationService {

    private final SalarySlipRepository salarySlipRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final PdfGenerationService pdfGenerationService;
    private final EmployeeRepository employeeRepository;
    private final SalaryFileStorageService salaryFileStorageService;

    public SalaryCalculationService(
            SalarySlipRepository salarySlipRepository,
            SalaryStructureRepository salaryStructureRepository,
            AttendanceRepository attendanceRepository,
            PdfGenerationService pdfGenerationService,
            EmployeeRepository employeeRepository,
            SalaryFileStorageService salaryFileStorageService) {

        this.salarySlipRepository = salarySlipRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.attendanceRepository = attendanceRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.employeeRepository = employeeRepository;
        this.salaryFileStorageService = salaryFileStorageService;
    }

    public SalarySlip generateSalary(
            String empId,
            int month,
            int year) {

        // Validate month
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid month");
        }

        // Validate that the requested month is completed
        YearMonth requestedMonth =
                YearMonth.of(year, month);

        YearMonth currentMonth =
                YearMonth.now(ZoneOffset.UTC);

        if (!requestedMonth.isBefore(currentMonth)) {
            throw new BadRequestException(
                    "Salary can only be generated for a completed month"
            );
        }

        // Get salary structure
        SalaryStructure structure =
                salaryStructureRepository
                        .findByEmpId(empId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Salary structure not found for employee: "
                                                + empId
                                )
                        );

        // Get employee attendance
        List<Attendance> attendances =
                attendanceRepository.findByEmpId(empId);

        // Attendance calculation
        int workingDays =
                SalaryCalculationUtil.calculateWorkingDays(
                        requestedMonth
                );

        int presentDays =
                SalaryCalculationUtil.calculatePresentDays(
                        attendances,
                        requestedMonth
                );

        int absentDays =
                SalaryCalculationUtil.calculateAbsentDays(
                        workingDays,
                        presentDays
                );

        // Salary calculation
        BigDecimal grossSalary =
                SalaryCalculationUtil.calculateGrossSalary(
                        structure.getBasic(),
                        structure.getHra(),
                        structure.getAllowances()
                );

        BigDecimal absentDeduction =
                SalaryCalculationUtil.calculateAbsentDeduction(
                        grossSalary,
                        workingDays,
                        absentDays
                );

        BigDecimal pfDeduction =
                SalaryCalculationUtil.calculatePf(
                        structure.getBasic(),
                        Boolean.TRUE.equals(
                                structure.getPfApplicable()
                        )
                );

        BigDecimal totalDeduction =
                SalaryCalculationUtil.calculateTotalDeductions(
                        absentDeduction,
                        pfDeduction
                );

        BigDecimal netSalary =
                SalaryCalculationUtil.calculateNetSalary(
                        grossSalary,
                        totalDeduction
                );

        // Create salary slip
        SalarySlip salarySlip = new SalarySlip();

        salarySlip.setEmpId(empId);
        salarySlip.setMonth(month);
        salarySlip.setYear(year);
        salarySlip.setWorkingDays(workingDays);
        salarySlip.setPresentDays(presentDays);
        salarySlip.setAbsentDays(absentDays);
        salarySlip.setGrossSalary(grossSalary);
        salarySlip.setDeduction(totalDeduction);
        salarySlip.setNetSalary(netSalary);

        // Get employee details
        Employee employee =
                employeeRepository
                        .findByEmpId(empId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found: " + empId
                                )
                        );

        // Prepare PDF data
        Map<String, Object> pdfData = new HashMap<>();

        pdfData.put(
                "month",
                requestedMonth.getMonth().toString()
        );

        pdfData.put("year", year);

        pdfData.put(
                "employeeName",
                employee.getFirstName()
                        + " "
                        + employee.getLastName()
        );

        pdfData.put("empId", employee.getEmpId());
        pdfData.put("department", "IT");

        pdfData.put("workingDays", workingDays);
        pdfData.put("presentDays", presentDays);
        pdfData.put("absentDays", absentDays);

        pdfData.put("basic", structure.getBasic());
        pdfData.put("hra", structure.getHra());
        pdfData.put("allowances", structure.getAllowances());
        pdfData.put("grossSalary", grossSalary);

        pdfData.put("pf", pfDeduction);
        pdfData.put("otherDeductions", BigDecimal.ZERO);
        pdfData.put("totalDeductions", totalDeduction);

        pdfData.put("netSalary", netSalary);

        byte[] pdf =
                pdfGenerationService.generatePdf(
                        "salary-slip",
                        pdfData
                );

        // Upload the PDF to MinIO and store only the object key
        String objectKey =
                salaryFileStorageService.uploadSalarySlip(
                        empId,
                        month,
                        year,
                        pdf
                );

        salarySlip.setPdfObjectKey(objectKey);

        return salarySlipRepository.save(salarySlip);
    }

    public SalarySlip getSalarySlip(
            String empId,
            int month,
            int year) {

        return salarySlipRepository
                .findByEmpIdAndMonthAndYear(
                        empId,
                        month,
                        year
                )
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salary slip not found for employee: "
                                        + empId
                        )
                );
    }

    public List<SalarySlip> viewSalarySlips(
            String email,
            String scope) {

        Employee employee =
                employeeRepository
                        .findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        if ("MY".equalsIgnoreCase(scope)) {

            return salarySlipRepository
                    .findByEmpId(employee.getEmpId());
        }

        if ("ALL".equalsIgnoreCase(scope)) {

            if (!"ADMIN".equalsIgnoreCase(
                    employee.getRole())) {

                throw new AccessDeniedException(
                        "Access denied. You are not Admin"
                );
            }

            return salarySlipRepository.findAll();
        }

        throw new BadRequestException(
                "Invalid salary slip scope. Use MY or ALL"
        );
    }
}