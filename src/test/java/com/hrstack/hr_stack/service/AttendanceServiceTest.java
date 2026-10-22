package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AttendanceResponse;
import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.OtpRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("AttendanceService")
class AttendanceServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final String EMP_ID = "HRStack_2025_IT_001";
    private static final UUID EMPLOYEE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private OtpRepository otpRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AttendanceService service;

    private static Employee employee(String role) {
        Employee e = new Employee(EMPLOYEE_ID, "Purvesh", "Test", EMAIL,
                "9999999999", "password", role);
        e.setEmpId(EMP_ID);
        return e;
    }

    // Otp with a FUTURE expiry so passwordEncoder.matches() gets reached
    private static com.hrstack.hr_stack.entity.Otp freshOtp() {
        long now = System.currentTimeMillis();
        com.hrstack.hr_stack.entity.Otp o = new com.hrstack.hr_stack.entity.Otp();
        o.setOtp("stored-hash");
        o.setCreatedOn(now - 60_000);
        o.setExpiredOn(now + 4 * 60_000);
        o.setDate(now);
        o.setDepartment("IT");
        return o;
    }

    // Otp already expired, used to trigger the "OTP is expired" branch
    private static com.hrstack.hr_stack.entity.Otp expiredOtp() {
        long now = System.currentTimeMillis();
        com.hrstack.hr_stack.entity.Otp o = new com.hrstack.hr_stack.entity.Otp();
        o.setOtp("stored-hash");
        o.setCreatedOn(now - 10 * 60_000);
        o.setExpiredOn(now - 5 * 60_000);
        o.setDate(now - 10 * 60_000);
        o.setDepartment("IT");
        return o;
    }

    private static void assertBadRequest(String expectedMessage, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertEquals(expectedMessage, ex.getMessage());
    }

    private static Page<AttendanceResponse> emptyPage() {
        return new PageImpl<>(List.of());
    }

    @Nested
    @DisplayName("markAttendance")
    class MarkAttendance {

        @Test
        @DisplayName("throws ResourceNotFound when employee email not found")
        void employeeMissing() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.markAttendance(EMAIL, "123456"));

            verifyNoInteractions(otpRepository);
            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws ResourceNotFound when no OTP has ever been generated")
        void noOtpExists() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(otpRepository.findTopByOrderByCreatedOnDesc()).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.markAttendance(EMAIL, "123456"));

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequest 'Invalid OTP' when code does not match")
        void invalidOtp() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(otpRepository.findTopByOrderByCreatedOnDesc()).thenReturn(Optional.of(freshOtp()));
            when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

            assertBadRequest("Invalid OTP",
                    () -> service.markAttendance(EMAIL, "000000"));

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequest 'OTP is expired' when now > expiresOn")
        void expiredOtpCase() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(otpRepository.findTopByOrderByCreatedOnDesc()).thenReturn(Optional.of(expiredOtp()));
            when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);

            assertBadRequest("OTP is expired",
                    () -> service.markAttendance(EMAIL, "123456"));

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("throws BadRequest 'already marked' when today's attendance exists")
        void alreadyMarkedToday() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(otpRepository.findTopByOrderByCreatedOnDesc()).thenReturn(Optional.of(freshOtp()));
            when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(true);

            assertBadRequest("Attendance already marked for today",
                    () -> service.markAttendance(EMAIL, "123456"));

            verify(attendanceRepository, never()).save(any());
        }

        @Test
        @DisplayName("saves PRESENT attendance with empId and current timestamp")
        void success() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(otpRepository.findTopByOrderByCreatedOnDesc()).thenReturn(Optional.of(freshOtp()));
            when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(false);
            when(attendanceRepository.save(any(Attendance.class)))
                    .thenAnswer(inv -> inv.getArgument(0, Attendance.class));

            long before = System.currentTimeMillis();
            Attendance result = service.markAttendance(EMAIL, "123456");
            long after = System.currentTimeMillis();

            assertEquals(EMP_ID, result.getEmpId());
            assertEquals("PRESENT", result.getStatus());
            assertTrue(result.getMarkedOn() >= before && result.getMarkedOn() <= after);

            ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
            verify(attendanceRepository).save(captor.capture());
            assertEquals(EMP_ID, captor.getValue().getEmpId());
            assertEquals("PRESENT", captor.getValue().getStatus());
        }
    }

    @Nested
    @DisplayName("viewAttendance")
    class ViewAttendance {

        @Test
        @DisplayName("throws ResourceNotFound when employee email not found")
        void employeeMissing() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.empty());

            assertThrows(ResourceNotFoundException.class,
                    () -> service.viewAttendance(EMAIL, "MY", "", null, null, 0, 10));
        }

        @Test
        @DisplayName("throws BadRequest when from > to")
        void fromAfterTo() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));

            assertBadRequest("'from' must not be after 'to'",
                    () -> service.viewAttendance(EMAIL, "MY", "", 200L, 100L, 0, 10));

            verifyNoInteractions(attendanceRepository);
        }

        @Test
        @DisplayName("scope=MY uses JWT empId and ignores search")
        void myScopeUsesJwtEmpId() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(attendanceRepository.findMine(eq(EMP_ID), anyLong(), anyLong(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            PageResponse<AttendanceResponse> response =
                    service.viewAttendance(EMAIL, "MY", "ignored-search", 100L, 200L, 0, 10);

            assertNotNull(response);
            verify(attendanceRepository).findMine(eq(EMP_ID), eq(100L), eq(200L), any(Pageable.class));
            verify(attendanceRepository, never()).searchAll(anyLong(), anyLong(), anyString(), any());
        }

        @Test
        @DisplayName("scope=MY null from/to become 0 and Long.MAX_VALUE")
        void myScopeDefaultsDateRange() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(attendanceRepository.findMine(eq(EMP_ID), eq(0L), eq(Long.MAX_VALUE), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.viewAttendance(EMAIL, "MY", "", null, null, 0, 10);

            verify(attendanceRepository).findMine(eq(EMP_ID), eq(0L), eq(Long.MAX_VALUE), any(Pageable.class));
        }

        @Test
        @DisplayName("scope=ALL denies EMPLOYEE with AccessDeniedException")
        void allScopeDeniesEmployee() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.viewAttendance(EMAIL, "ALL", "", 0L, 200L, 0, 10));

            assertEquals("Access denied. You are not Admin", ex.getMessage());
            verifyNoInteractions(attendanceRepository);
        }

        @Test
        @DisplayName("scope=ALL allows ADMIN and calls searchAll")
        void allScopeAllowsAdmin() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("ADMIN")));
            when(attendanceRepository.searchAll(eq(0L), eq(200L), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.viewAttendance(EMAIL, "ALL", "purvesh", 0L, 200L, 0, 10);

            verify(attendanceRepository).searchAll(eq(0L), eq(200L), eq("%purvesh%"), any(Pageable.class));
        }

        @Test
        @DisplayName("scope=ALL truncates search longer than 50 chars")
        void allScopeTruncatesSearch() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("ADMIN")));
            when(attendanceRepository.searchAll(anyLong(), anyLong(), anyString(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            String longSearch = "a".repeat(80);
            service.viewAttendance(EMAIL, "ALL", longSearch, 0L, 200L, 0, 10);

            ArgumentCaptor<String> patCaptor = ArgumentCaptor.forClass(String.class);
            verify(attendanceRepository).searchAll(anyLong(), anyLong(), patCaptor.capture(), any());
            assertEquals("%" + "a".repeat(50) + "%", patCaptor.getValue());
        }

        @Test
        @DisplayName("invalid scope is rejected")
        void invalidScope() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));

            assertBadRequest("Invalid attendance scope. Use MY or ALL",
                    () -> service.viewAttendance(EMAIL, "SHARED", "", 0L, 200L, 0, 10));

            verifyNoInteractions(attendanceRepository);
        }

        @Test
        @DisplayName("page size is clamped to 50 and negative page to 0")
        void pageSizeClamped() {
            when(employeeRepository.findByEmail(EMAIL)).thenReturn(Optional.of(employee("EMPLOYEE")));
            when(attendanceRepository.findMine(eq(EMP_ID), anyLong(), anyLong(), any(Pageable.class)))
                    .thenReturn(emptyPage());

            service.viewAttendance(EMAIL, "MY", "", null, null, -3, 999);

            ArgumentCaptor<Pageable> pageCaptor = ArgumentCaptor.forClass(Pageable.class);
            verify(attendanceRepository).findMine(eq(EMP_ID), anyLong(), anyLong(), pageCaptor.capture());
            assertEquals(0, pageCaptor.getValue().getPageNumber());
            assertEquals(50, pageCaptor.getValue().getPageSize());
        }
    }
}