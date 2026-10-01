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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
public class SalaryCalculationService {

    private final TempFileStorageService tempFileStorageService;
    private final SalarySlipRepository salarySlipRepository;
    private final SalaryStructureRepository salaryStructureRepository;
    private final AttendanceRepository attendanceRepository;
    private final PdfGenerationService pdfGenerationService;
    private final EmployeeRepository employeeRepository;
    private final SalaryFileStorageService salaryFileStorageService;
    private final InAppNotificationService inAppNotificationService;

    @Value("${salary-slip.replacement-window-months}")
    private long replacementWindowMonths;

    public SalaryCalculationService(
            SalarySlipRepository salarySlipRepository,
            SalaryStructureRepository salaryStructureRepository,
            AttendanceRepository attendanceRepository,
            PdfGenerationService pdfGenerationService,
            EmployeeRepository employeeRepository,
            SalaryFileStorageService salaryFileStorageService,
            TempFileStorageService tempFileStorageService,
            InAppNotificationService inAppNotificationService) {
        this.salarySlipRepository = salarySlipRepository;
        this.salaryStructureRepository = salaryStructureRepository;
        this.attendanceRepository = attendanceRepository;
        this.pdfGenerationService = pdfGenerationService;
        this.employeeRepository = employeeRepository;
        this.salaryFileStorageService = salaryFileStorageService;
        this.tempFileStorageService = tempFileStorageService;
        this.inAppNotificationService = inAppNotificationService;
    }

    public SalarySlip generateSalary(String empId,int month,int year) {
        if (month < 1 || month > 12) {
            throw new BadRequestException("Invalid month");
        }

        YearMonth requestedMonth = YearMonth.of(year,month);
        YearMonth currentMonth = YearMonth.now(ZoneOffset.UTC);

        if (!requestedMonth.isBefore(currentMonth)) {
            throw new BadRequestException(
                    "Salary can only be generated for a completed month"
            );
        }

        if (salarySlipRepository.findByEmpIdAndMonthAndYear(empId,month,year).isPresent()) {
            throw new BadRequestException(
                    "Salary slip already exists for employee "
                            + empId
                            + " for "
                            + month
                            + "/"
                            + year
            );
        }

        SalaryStructure structure =
                salaryStructureRepository.findByEmpId(empId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Salary structure not found for employee: "
                                                + empId
                                )
                        );

        List<Attendance> attendances =
                attendanceRepository.findByEmpId(empId);

        int workingDays =
                SalaryCalculationUtil.calculateWorkingDays(requestedMonth);

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
                        Boolean.TRUE.equals(structure.getPfApplicable())
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

        Employee employee =
                employeeRepository.findByEmpId(empId)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found: " + empId
                                )
                        );

        Map<String,Object> pdfData = new HashMap<>();

        pdfData.put(
                "month",
                requestedMonth.getMonth().toString()
        );
        pdfData.put("year",year);
        pdfData.put(
                "employeeName",
                employee.getFirstName() + " " + employee.getLastName()
        );
        pdfData.put("empId",employee.getEmpId());
        pdfData.put("department","IT");
        pdfData.put("workingDays",workingDays);
        pdfData.put("presentDays",presentDays);
        pdfData.put("absentDays",absentDays);
        pdfData.put("basic",structure.getBasic());
        pdfData.put("hra",structure.getHra());
        pdfData.put("allowances",structure.getAllowances());
        pdfData.put("grossSalary",grossSalary);
        pdfData.put("pf",pfDeduction);
        pdfData.put("otherDeductions",BigDecimal.ZERO);
        pdfData.put("totalDeductions",totalDeduction);
        pdfData.put("netSalary",netSalary);

        byte[] pdf =
                pdfGenerationService.generatePdf(
                        "salary-slip",
                        pdfData
                );

        String objectKey =
                tempFileStorageService.uploadSalarySlipToTemp(
                        empId,
                        month,
                        year,
                        pdf
                );

        salarySlip.setPdfObjectKey(objectKey);
        salarySlip.setGeneratedAt(System.currentTimeMillis());

        SalarySlip savedSalarySlip =
                salarySlipRepository.save(salarySlip);

        String monthName =
                requestedMonth.getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.ENGLISH
                        );

        inAppNotificationService.createNotification(
                employee.getId(),
                "SALARY_SLIP_GENERATED",
                "Your salary slip for "
                        + monthName
                        + " "
                        + year
                        + " has been generated."
        );

        return savedSalarySlip;
    }

    public SalarySlip getSalarySlip(String empId,int month,int year) {
        return salarySlipRepository
                .findByEmpIdAndMonthAndYear(empId,month,year)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Salary slip not found for employee: "
                                        + empId
                        )
                );
    }

    public SalarySlip updateSalarySlip(SalarySlip salarySlip) {
        return salarySlipRepository.save(salarySlip);
    }

    public void notifySalarySlipReplaced(
            SalarySlip salarySlip) {

        Employee employee =
                employeeRepository.findByEmpId(
                        salarySlip.getEmpId()
                ).orElseThrow(() ->
                        new ResourceNotFoundException(
                                "Employee not found: "
                                        + salarySlip.getEmpId()
                        )
                );

        YearMonth salaryMonth =
                YearMonth.of(
                        salarySlip.getYear(),
                        salarySlip.getMonth()
                );

        String monthName =
                salaryMonth.getMonth()
                        .getDisplayName(
                                TextStyle.FULL,
                                Locale.ENGLISH
                        );

        inAppNotificationService.createNotification(
                employee.getId(),
                "SALARY_SLIP_REPLACED",
                "Your salary slip for "
                        + monthName
                        + " "
                        + salarySlip.getYear()
                        + " has been updated."
        );
    }

    public List<SalarySlip> viewSalarySlips(String email,String scope) {
        Employee employee =
                employeeRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException(
                                        "Employee not found"
                                )
                        );

        List<SalarySlip> salarySlips;

        if ("MY".equalsIgnoreCase(scope)) {
            salarySlips =
                    salarySlipRepository.findByEmpId(employee.getEmpId());
        } else if ("ALL".equalsIgnoreCase(scope)) {
            if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
                throw new AccessDeniedException(
                        "Access denied. You are not Admin"
                );
            }
            salarySlips = salarySlipRepository.findAll();
        } else {
            throw new BadRequestException(
                    "Invalid salary slip scope. Use MY or ALL"
            );
        }

        salarySlips.forEach(salarySlip -> {
            if (salarySlip.getPdfObjectKey() != null) {
                String signedUrl =
                        salaryFileStorageService
                                .getSalarySlipSignedUrlFromEitherBucket(
                                        salarySlip.getPdfObjectKey()
                                );
                salarySlip.setPdfUrl(signedUrl);
            }

            if (salarySlip.getGeneratedAt() != null) {
                long replacementWindow =
                        replacementWindowMonths
                                * 30L
                                * 24
                                * 60
                                * 60
                                * 1000;

                long expiryTime =
                        salarySlip.getGeneratedAt()
                                + replacementWindow;

                salarySlip.setReplaceAllowed(
                        System.currentTimeMillis() <= expiryTime
                );
            } else {
                salarySlip.setReplaceAllowed(false);
            }
        });

        return salarySlips;
    }
}