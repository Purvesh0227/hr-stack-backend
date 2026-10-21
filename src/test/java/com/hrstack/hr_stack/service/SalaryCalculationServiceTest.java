package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.SalarySlip;
import com.hrstack.hr_stack.entity.SalaryStructure;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.SalarySlipRepository;
import com.hrstack.hr_stack.repository.SalaryStructureRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.YearMonth;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("SalaryCalculationService.generateSalary")
class SalaryCalculationServiceTest {

    // Fixed past month, so tests never depend on today's date.
    private static final String EMP_ID = "EMP001";
    private static final int MONTH = 1;
    private static final int YEAR = 2025;                 // January 2025 = 23 working days
    private static final UUID EMPLOYEE_UUID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final byte[] PDF_BYTES = {1, 2, 3};

    @Mock private SalarySlipRepository salarySlipRepository;
    @Mock private SalaryStructureRepository salaryStructureRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private PdfGenerationService pdfGenerationService;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private SalaryFileStorageService salaryFileStorageService;
    @Mock private TempFileStorageService tempFileStorageService;
    @Mock private InAppNotificationService inAppNotificationService;

    @InjectMocks
    private SalaryCalculationService service;

    // ---------- helpers ----------

    private static void assertMoney(String expected, BigDecimal actual) {
        assertTrue(new BigDecimal(expected).compareTo(actual) == 0,
                "expected " + expected + " but was " + actual);
    }

    private static SalaryStructure structure(boolean pfApplicable) {
        SalaryStructure s = new SalaryStructure();
        s.setEmpId(EMP_ID);
        s.setBasic(new BigDecimal("30000"));
        s.setHra(new BigDecimal("12000"));
        s.setAllowances(new BigDecimal("3000"));
        s.setPfApplicable(pfApplicable);
        return s;
    }

    private static Employee employee() {
        Employee e = new Employee();
        e.setId(EMPLOYEE_UUID);
        e.setEmpId(EMP_ID);
        e.setFirstName("Purvesh");
        e.setLastName("Test");
        return e;
    }

    /** 20 PRESENT days in January 2025 (so 23 working - 20 present = 3 absent). */
    private static List<Attendance> twentyPresentDaysInJanuary() {
        List<Attendance> list = new ArrayList<>();
        for (int day = 1; day <= 20; day++) {
            Attendance a = new Attendance();
            a.setStatus("PRESENT");
            a.setMarkedOn(Instant.parse(String.format("2025-01-%02dT09:00:00Z", day)).toEpochMilli());
            list.add(a);
        }
        return list;
    }

    /** Sets up a complete valid scenario. Every stub here is used by generateSalary. */
    private void arrangeValidScenario(boolean pfApplicable) {
        when(salarySlipRepository.findByEmpIdAndMonthAndYear(EMP_ID, MONTH, YEAR))
                .thenReturn(Optional.empty());
        when(salaryStructureRepository.findByEmpId(EMP_ID))
                .thenReturn(Optional.of(structure(pfApplicable)));
        when(attendanceRepository.findByEmpId(EMP_ID))
                .thenReturn(twentyPresentDaysInJanuary());
        when(employeeRepository.findByEmpId(EMP_ID))
                .thenReturn(Optional.of(employee()));
        when(pdfGenerationService.generatePdf(eq("salary-slip"), anyMap()))
                .thenReturn(PDF_BYTES);
        when(tempFileStorageService.uploadSalarySlipToTemp(EMP_ID, MONTH, YEAR, PDF_BYTES))
                .thenReturn("temp/salary-slips/EMP001.pdf");
        when(salarySlipRepository.save(any(SalarySlip.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    // ---------- validation rules ----------

    @ParameterizedTest(name = "month {0} is rejected")
    @ValueSource(ints = {0, 13, -1})
    void generateSalary_whenMonthIsInvalid_throwsBadRequest(int month) {
        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.generateSalary(EMP_ID, month, YEAR));

        assertEquals("Invalid month", ex.getMessage());
        verifyNoInteractions(salarySlipRepository);
    }

    @Test
    void generateSalary_whenMonthIsCurrentMonth_throwsBadRequest() {
        YearMonth current = YearMonth.now(ZoneOffset.UTC);

        assertThrows(BadRequestException.class,
                () -> service.generateSalary(EMP_ID, current.getMonthValue(), current.getYear()));

        verify(salarySlipRepository, never()).save(any());
    }

    @Test
    void generateSalary_whenMonthIsInFuture_throwsBadRequest() {
        YearMonth next = YearMonth.now(ZoneOffset.UTC).plusMonths(1);

        assertThrows(BadRequestException.class,
                () -> service.generateSalary(EMP_ID, next.getMonthValue(), next.getYear()));

        verify(salarySlipRepository, never()).save(any());
    }

    @Test
    void generateSalary_whenSlipAlreadyExists_throwsBadRequestAndDoesNotSave() {
        when(salarySlipRepository.findByEmpIdAndMonthAndYear(EMP_ID, MONTH, YEAR))
                .thenReturn(Optional.of(new SalarySlip()));

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.generateSalary(EMP_ID, MONTH, YEAR));

        assertTrue(ex.getMessage().contains("already exists"));
        verify(salarySlipRepository, never()).save(any());
    }

    @Test
    void generateSalary_whenSalaryStructureMissing_throwsNotFound() {
        when(salarySlipRepository.findByEmpIdAndMonthAndYear(EMP_ID, MONTH, YEAR))
                .thenReturn(Optional.empty());
        when(salaryStructureRepository.findByEmpId(EMP_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.generateSalary(EMP_ID, MONTH, YEAR));

        assertTrue(ex.getMessage().contains("Salary structure not found"));
    }

    @Test
    void generateSalary_whenEmployeeMissing_throwsNotFoundAndGeneratesNoPdf() {
        when(salarySlipRepository.findByEmpIdAndMonthAndYear(EMP_ID, MONTH, YEAR))
                .thenReturn(Optional.empty());
        when(salaryStructureRepository.findByEmpId(EMP_ID))
                .thenReturn(Optional.of(structure(true)));
        when(attendanceRepository.findByEmpId(EMP_ID))
                .thenReturn(List.of());
        when(employeeRepository.findByEmpId(EMP_ID))
                .thenReturn(Optional.empty());

        ResourceNotFoundException ex = assertThrows(ResourceNotFoundException.class,
                () -> service.generateSalary(EMP_ID, MONTH, YEAR));

        assertTrue(ex.getMessage().contains("Employee not found"));
        verifyNoInteractions(pdfGenerationService);
    }

    // ---------- happy paths ----------

    @Test
    void generateSalary_whenAllValidAndPfApplicable_savesCorrectSlipAndNotifies() {
        arrangeValidScenario(true);

        SalarySlip result = service.generateSalary(EMP_ID, MONTH, YEAR);

        assertEquals(EMP_ID, result.getEmpId());
        assertEquals(23, result.getWorkingDays());
        assertEquals(20, result.getPresentDays());
        assertEquals(3, result.getAbsentDays());
        assertMoney("45000.00", result.getGrossSalary());
        assertMoney("9469.56", result.getDeduction());     // 5869.56 absent + 3600.00 PF
        assertMoney("35530.44", result.getNetSalary());
        assertEquals("temp/salary-slips/EMP001.pdf", result.getPdfObjectKey());
        assertNotNull(result.getGeneratedAt());

        verify(inAppNotificationService).createNotification(
                EMPLOYEE_UUID,
                "SALARY_SLIP_GENERATED",
                "Your salary slip for January 2025 has been generated.");
    }

    @Test
    void generateSalary_whenPfNotApplicable_deductsOnlyAbsentDays() {
        arrangeValidScenario(false);

        SalarySlip result = service.generateSalary(EMP_ID, MONTH, YEAR);

        assertMoney("5869.56", result.getDeduction());     // absent deduction only
        assertMoney("39130.44", result.getNetSalary());
    }
}