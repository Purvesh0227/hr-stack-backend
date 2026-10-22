package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrMarkResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionStatusResponse;
import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.QrAttendanceSession;
import com.hrstack.hr_stack.exception.AccessDeniedException;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.QrAttendanceException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AttendanceRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.QrAttendanceSessionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrAttendanceService")
class QrAttendanceServiceTest {

    private static final String ADMIN_EMAIL = "admin@example.com";
    private static final String EMP_EMAIL   = "emp@example.com";
    private static final String EMP_ID      = "HRStack_2025_IT_001";
    private static final UUID SESSION_ID =
            UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID ADMIN_ID =
            UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID EMPLOYEE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");

    private static final ZoneId ZONE = ZoneId.of("Asia/Kolkata");

    @Mock private QrAttendanceSessionRepository sessionRepository;
    @Mock private AttendanceRepository attendanceRepository;
    @Mock private EmployeeRepository employeeRepository;

    @InjectMocks
    private QrAttendanceService service;

    @BeforeEach
    void fixTimezone() {
        // QrAttendanceService reads app.attendance.zone via @Value
        ReflectionTestUtils.setField(service, "zone", ZONE);
    }

    // ---------- helpers ----------

    private static Employee admin() {
        Employee e = new Employee(ADMIN_ID, "Admin", "One", ADMIN_EMAIL,
                "9000000000", "password", "ADMIN");
        e.setEmpId("HRStack_2025_IT_900");
        e.setStatus("ACTIVE");
        return e;
    }

    private static Employee activeEmployee() {
        Employee e = new Employee(EMPLOYEE_ID, "testing", "Test", EMP_EMAIL,
                "9999999999", "password", "EMPLOYEE");
        e.setEmpId(EMP_ID);
        e.setStatus("ACTIVE");
        return e;
    }

    /** Active, unexpired session whose tokenHash = sha256("raw-token"). */
    private static QrAttendanceSession liveSession() {
        long now = System.currentTimeMillis();
        QrAttendanceSession s = new QrAttendanceSession();
        ReflectionTestUtils.setField(s, "uuid", SESSION_ID);
        s.setTokenHash(sha256Hex("raw-token"));
        s.setCreatedBy(ADMIN_ID);
        s.setDepartment("IT");
        s.setCreatedOn(now - 60_000);
        s.setExpiresOn(now + 4 * 60_000);
        s.setStatus(QrAttendanceSession.STATUS_ACTIVE);
        return s;
    }

    private static QrAttendanceException assertQr(HttpStatus status, String code,
                                                  Executable call) {
        QrAttendanceException ex = assertThrows(QrAttendanceException.class, call);
        assertEquals(status, ex.getStatus(), "HTTP status");
        assertEquals(code,   ex.getCode(),   "error code");
        return ex;
    }

    private static long startOfTodayInZone() {
        return LocalDate.now(ZONE).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }
    private static long endOfTodayInZone() {
        return LocalDate.now(ZONE).plusDays(1).atStartOfDay(ZONE).toInstant().toEpochMilli();
    }

    private static String sha256Hex(String value) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(d.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    // ============================================================
    // createSession
    // ============================================================
    @Nested
    @DisplayName("createSession")
    class CreateSession {

        @Test
        @DisplayName("non-admin gets AccessDeniedException")
        void nonAdminDenied() {
            when(employeeRepository.findByEmail(EMP_EMAIL))
                    .thenReturn(Optional.of(activeEmployee()));

            AccessDeniedException ex = assertThrows(AccessDeniedException.class,
                    () -> service.createSession(EMP_EMAIL, "IT"));
            assertEquals("Access denied. You are not Admin", ex.getMessage());

            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("blank department is rejected")
        void blankDepartment() {
            when(employeeRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.createSession(ADMIN_EMAIL, "  "));
            assertEquals("Department is required.", ex.getMessage());
        }

        @Test
        @DisplayName("department longer than 100 chars is rejected")
        void longDepartment() {
            when(employeeRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.createSession(ADMIN_EMAIL, "X".repeat(101)));
            assertEquals("Department is too long.", ex.getMessage());
        }

        @Test
        @DisplayName("success: revokes old active, stores only hash, returns 43-char token, TTL=5min")
        void success() {
            when(employeeRepository.findByEmail(ADMIN_EMAIL)).thenReturn(Optional.of(admin()));
            when(sessionRepository.revokeActiveByDepartment("IT")).thenReturn(1);
            when(sessionRepository.save(any(QrAttendanceSession.class)))
                    .thenAnswer(inv -> {
                        QrAttendanceSession s = inv.getArgument(0);
                        ReflectionTestUtils.setField(s, "uuid", SESSION_ID);
                        return s;
                    });

            QrSessionResponse response = service.createSession(ADMIN_EMAIL, "IT");

            // token shape
            assertNotNull(response.token());
            assertEquals(43, response.token().length());   // 32 bytes -> base64url no pad
            assertEquals("IT", response.department());
            assertEquals(SESSION_ID, response.sessionId());

            // persisted session: hash only, 5-minute TTL, ACTIVE
            ArgumentCaptor<QrAttendanceSession> captor =
                    ArgumentCaptor.forClass(QrAttendanceSession.class);
            verify(sessionRepository).save(captor.capture());
            QrAttendanceSession saved = captor.getValue();
            assertEquals(sha256Hex(response.token()), saved.getTokenHash());
            assertNotEquals(response.token(), saved.getTokenHash());      // never raw
            assertEquals(5 * 60_000L, saved.getExpiresOn() - saved.getCreatedOn());
            assertEquals(QrAttendanceSession.STATUS_ACTIVE, saved.getStatus());
            assertEquals(ADMIN_ID, saved.getCreatedBy());
            verify(sessionRepository).revokeActiveByDepartment("IT");
        }
    }

    // ============================================================
    // getSessionStatus
    // ============================================================
    @Nested
    @DisplayName("getSessionStatus")
    class GetSessionStatus {

        @Test
        @DisplayName("missing session -> ResourceNotFound")
        void notFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.getSessionStatus(SESSION_ID));
        }

        @Test
        @DisplayName("REVOKED wins over EXPIRED/ACTIVE")
        void revoked() {
            QrAttendanceSession s = liveSession();
            s.setStatus(QrAttendanceSession.STATUS_REVOKED);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(attendanceRepository.countBySessionId(SESSION_ID)).thenReturn(0L);

            assertEquals("REVOKED", service.getSessionStatus(SESSION_ID).status());
        }

        @Test
        @DisplayName("past expiry with ACTIVE status is reported EXPIRED")
        void expired() {
            QrAttendanceSession s = liveSession();
            s.setExpiresOn(System.currentTimeMillis() - 1_000);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(attendanceRepository.countBySessionId(SESSION_ID)).thenReturn(3L);

            QrSessionStatusResponse r = service.getSessionStatus(SESSION_ID);
            assertEquals("EXPIRED", r.status());
            assertEquals(3L, r.scanCount());
        }

        @Test
        @DisplayName("live session reported ACTIVE with scan count")
        void active() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(liveSession()));
            when(attendanceRepository.countBySessionId(SESSION_ID)).thenReturn(7L);

            QrSessionStatusResponse r = service.getSessionStatus(SESSION_ID);
            assertEquals("ACTIVE", r.status());
            assertEquals(7L, r.scanCount());
        }
    }

    // ============================================================
    // revokeSession
    // ============================================================
    @Nested
    @DisplayName("revokeSession")
    class RevokeSession {

        @Test
        @DisplayName("sets status to REVOKED and saves")
        void revokes() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));
            when(sessionRepository.save(s)).thenReturn(s);

            service.revokeSession(SESSION_ID);

            assertEquals(QrAttendanceSession.STATUS_REVOKED, s.getStatus());
            verify(sessionRepository).save(s);
        }

        @Test
        @DisplayName("missing session -> ResourceNotFound")
        void notFound() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.revokeSession(SESSION_ID));
        }
    }

    // ============================================================
    // markAttendance
    // ============================================================
    @Nested
    @DisplayName("markAttendance")
    class MarkAttendance {

        @Test
        @DisplayName("blank token -> QR_INVALID 400")
        void blankToken() {
            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.markAttendance(EMP_EMAIL, "  "));
            verifyNoInteractions(sessionRepository, employeeRepository);
        }

        @Test
        @DisplayName("token longer than 100 chars -> QR_INVALID 400")
        void longToken() {
            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.markAttendance(EMP_EMAIL, "a".repeat(101)));
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("unknown token hash -> QR_INVALID 400")
        void unknownToken() {
            when(sessionRepository.findByTokenHash(anyString())).thenReturn(Optional.empty());

            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.markAttendance(EMP_EMAIL, "unknown-token"));
            verifyNoInteractions(employeeRepository);
        }

        @Test
        @DisplayName("revoked session -> QR_REVOKED 410")
        void revoked() {
            QrAttendanceSession s = liveSession();
            s.setStatus(QrAttendanceSession.STATUS_REVOKED);
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));

            assertQr(HttpStatus.GONE, "QR_REVOKED",
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));
            verifyNoInteractions(attendanceRepository);
        }

        @Test
        @DisplayName("expired session -> QR_EXPIRED 410")
        void expired() {
            QrAttendanceSession s = liveSession();
            s.setExpiresOn(System.currentTimeMillis() - 1_000);
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));

            assertQr(HttpStatus.GONE, "QR_EXPIRED",
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));
            verifyNoInteractions(attendanceRepository);
        }

        @Test
        @DisplayName("inactive account -> NOT_ELIGIBLE 403")
        void inactive() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));
            Employee emp = activeEmployee();
            emp.setStatus("PENDING");
            when(employeeRepository.findByEmail(EMP_EMAIL)).thenReturn(Optional.of(emp));

            assertQr(HttpStatus.FORBIDDEN, "NOT_ELIGIBLE",
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));
        }

        @Test
        @DisplayName("employee already marked today -> ALREADY_MARKED 409")
        void alreadyMarked() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));
            when(employeeRepository.findByEmail(EMP_EMAIL))
                    .thenReturn(Optional.of(activeEmployee()));
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(true);

            assertQr(HttpStatus.CONFLICT, "ALREADY_MARKED",
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));

            verify(attendanceRepository, never()).saveAndFlush(any());
        }

        @Test
        @DisplayName("DB race (unique index) -> ALREADY_MARKED 409")
        void dbRace() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));
            when(employeeRepository.findByEmail(EMP_EMAIL))
                    .thenReturn(Optional.of(activeEmployee()));
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(false);
            when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                    .thenThrow(new DataIntegrityViolationException("dup"));

            assertQr(HttpStatus.CONFLICT, "ALREADY_MARKED",
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));
        }

        @Test
        @DisplayName("success: sets method=QR, sessionId, attendanceDate, status PRESENT")
        void success() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));
            when(employeeRepository.findByEmail(EMP_EMAIL))
                    .thenReturn(Optional.of(activeEmployee()));
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(false);
            when(attendanceRepository.saveAndFlush(any(Attendance.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            QrMarkResponse result = service.markAttendance(EMP_EMAIL, "raw-token");

            assertEquals("PRESENT", result.status());
            assertEquals("QR",      result.method());

            ArgumentCaptor<Attendance> captor = ArgumentCaptor.forClass(Attendance.class);
            verify(attendanceRepository).saveAndFlush(captor.capture());
            Attendance saved = captor.getValue();
            assertEquals(EMP_ID, saved.getEmpId());
            assertEquals("PRESENT", saved.getStatus());
            assertEquals("QR", saved.getMethod());
            assertEquals(SESSION_ID, saved.getSessionId());
            assertEquals(LocalDate.now(ZONE), saved.getAttendanceDate());
        }

        @Test
        @DisplayName("day window uses the configured attendance zone")
        void windowUsesConfiguredZone() {
            QrAttendanceSession s = liveSession();
            when(sessionRepository.findByTokenHash(sha256Hex("raw-token")))
                    .thenReturn(Optional.of(s));
            when(employeeRepository.findByEmail(EMP_EMAIL))
                    .thenReturn(Optional.of(activeEmployee()));
            when(attendanceRepository.existsByEmpIdAndMarkedOnBetween(
                    eq(EMP_ID), any(), any())).thenReturn(true);

            assertThrows(QrAttendanceException.class,
                    () -> service.markAttendance(EMP_EMAIL, "raw-token"));

            verify(attendanceRepository).existsByEmpIdAndMarkedOnBetween(
                    EMP_ID, startOfTodayInZone(), endOfTodayInZone());
        }
    }
}