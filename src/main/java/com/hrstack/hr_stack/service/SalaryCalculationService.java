package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.PageResponse;
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
import com.hrstack.hr_stack.util.SearchUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.data.domain.Pageable;
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

    private static final int MAX_PAGE_SIZE = 50;
    private static final int MAX_SEARCH_LENGTH = 50;

    public PageResponse<SalarySlip> viewSalarySlips(
            String email,
            String scope,
            String search,
            Integer month,
            Integer year,
            int page,
            int size) {

        Employee employee =
                employeeRepository.findByEmail(email)
                        .orElseThrow(() ->
                                new ResourceNotFoundException("Employee not found"));

        int safeMonth = month == null ? 0 : month;
        if (safeMonth < 0 || safeMonth > 12) {
            throw new BadRequestException("Month must be between 1 and 12.");
        }

        int safeYear = year == null ? 0 : year;
        if (safeYear != 0 && (safeYear < 2000 || safeYear > 2100)) {
            throw new BadRequestException("Year must be between 2000 and 2100.");
        }

        // Sorting is inside the repository query
        Pageable pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), MAX_PAGE_SIZE)
        );

        Page<SalarySlip> result;

        if ("MY".equalsIgnoreCase(scope)) {
            // identity comes from the JWT, search is ignored
            result = salarySlipRepository.findMine(
                    employee.getEmpId(), safeMonth, safeYear, pageable);

        } else if ("ALL".equalsIgnoreCase(scope)) {

            if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
                throw new AccessDeniedException("Access denied. You are not Admin");
            }

            String term = search == null ? "" : search.trim();
            if (term.length() > MAX_SEARCH_LENGTH) {
                term = term.substring(0, MAX_SEARCH_LENGTH);
            }

            result = salarySlipRepository.searchAll(
                    safeMonth, safeYear,
                    SearchUtils.toLikePattern(term),
                    pageable);

        } else {
            throw new BadRequestException("Invalid salary slip scope. Use MY or ALL");
        }

        // Signed link + replace flag only for the rows on this page
        result.getContent().forEach(this::enrichSalarySlip);

        return PageResponse.from(result);
    }

    private void enrichSalarySlip(SalarySlip salarySlip) {
        if (salarySlip.getPdfObjectKey() != null) {
            String signedUrl =
                    salaryFileStorageService
                            .getSalarySlipSignedUrlFromEitherBucket(
                                    salarySlip.getPdfObjectKey());
            salarySlip.setPdfUrl(signedUrl);
        }

        if (salarySlip.getGeneratedAt() != null) {
            long replacementWindow =
                    replacementWindowMonths * 30L * 24 * 60 * 60 * 1000;

            long expiryTime = salarySlip.getGeneratedAt() + replacementWindow;

            salarySlip.setReplaceAllowed(System.currentTimeMillis() <= expiryTime);
        } else {
            salarySlip.setReplaceAllowed(false);
        }
    }
}