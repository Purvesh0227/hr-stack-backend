package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrEmailResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.QrAttendanceSession;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.QrAttendanceException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.QrAttendanceSessionRepository;
import com.hrstack.hr_stack.service.QrAttendanceEmailSender.Recipient;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("QrAttendanceEmailService")
class QrAttendanceEmailServiceTest {

    private static final UUID SESSION_ID =
            UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID EMP_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String TOKEN = "raw-token";
    private static final String FRONTEND = "http://localhost:5173/";
    private static final String BACKEND  = "http://localhost:8080/";

    @Mock private QrAttendanceSessionRepository sessionRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private QrAttendanceEmailSender emailSender;

    private QrAttendanceEmailService service;

    @BeforeEach
    void setUp() {
        service = new QrAttendanceEmailService(
                sessionRepository,
                employeeRepository,
                emailSender,
                FRONTEND,
                BACKEND,
                "Asia/Kolkata",
                ""          // blank = send to all active employees
        );
    }

    // ---------- helpers ----------

    private static String sha256Hex(String value) {
        try {
            MessageDigest d = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(d.digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static QrAttendanceSession liveSession() {
        long now = System.currentTimeMillis();
        QrAttendanceSession s = new QrAttendanceSession();
        ReflectionTestUtils.setField(s, "uuid", SESSION_ID);
        s.setTokenHash(sha256Hex(TOKEN));
        s.setDepartment("IT");
        s.setCreatedOn(now - 60_000);
        s.setExpiresOn(now + 4 * 60_000);
        s.setStatus(QrAttendanceSession.STATUS_ACTIVE);
        return s;
    }

    private static Employee activeEmployee() {
        Employee e = new Employee(EMP_ID, "testing", "Test",
                "emp@example.com", "9999999999", "password", "EMPLOYEE");
        e.setEmpId("HRStack_2025_IT_001");
        e.setStatus("ACTIVE");
        return e;
    }

    private static QrAttendanceException assertQr(HttpStatus status, String code,
                                                  Executable call) {
        QrAttendanceException ex = assertThrows(QrAttendanceException.class, call);
        assertEquals(status, ex.getStatus());
        assertEquals(code, ex.getCode());
        return ex;
    }

    // ============================================================
    // token / session pre-flight
    // ============================================================
    @Nested
    @DisplayName("token and session validation")
    class PreFlight {

        @Test
        @DisplayName("blank token -> QR_INVALID 400, no DB lookup")
        void blankToken() {
            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.sendToAllEmployees(SESSION_ID, "  "));
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("token > 100 chars -> QR_INVALID 400")
        void longToken() {
            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.sendToAllEmployees(SESSION_ID, "a".repeat(101)));
            verifyNoInteractions(sessionRepository);
        }

        @Test
        @DisplayName("session id not found -> ResourceNotFound")
        void sessionMissing() {
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.empty());
            assertThrows(ResourceNotFoundException.class,
                    () -> service.sendToAllEmployees(SESSION_ID, TOKEN));
        }

        @Test
        @DisplayName("token hash mismatch -> QR_INVALID 400")
        void wrongToken() {
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(liveSession()));

            assertQr(HttpStatus.BAD_REQUEST, "QR_INVALID",
                    () -> service.sendToAllEmployees(SESSION_ID, "wrong-token"));

            verifyNoInteractions(employeeRepository);
        }

        @Test
        @DisplayName("revoked session -> QR_REVOKED 410")
        void revoked() {
            QrAttendanceSession s = liveSession();
            s.setStatus(QrAttendanceSession.STATUS_REVOKED);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));

            assertQr(HttpStatus.GONE, "QR_REVOKED",
                    () -> service.sendToAllEmployees(SESSION_ID, TOKEN));

            verifyNoInteractions(employeeRepository);
            verify(sessionRepository, never()).markEmailed(any(), anyLong());
        }

        @Test
        @DisplayName("expired session -> QR_EXPIRED 410")
        void expired() {
            QrAttendanceSession s = liveSession();
            s.setExpiresOn(System.currentTimeMillis() - 1_000);
            when(sessionRepository.findById(SESSION_ID)).thenReturn(Optional.of(s));

            assertQr(HttpStatus.GONE, "QR_EXPIRED",
                    () -> service.sendToAllEmployees(SESSION_ID, TOKEN));

            verifyNoInteractions(employeeRepository);
            verify(sessionRepository, never()).markEmailed(any(), anyLong());
        }

        @Test
        @DisplayName("no active employees -> BadRequest 'No active employees to email.'")
        void noRecipients() {
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(liveSession()));
            when(employeeRepository.findByStatusIgnoreCase("ACTIVE"))
                    .thenReturn(List.of());

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.sendToAllEmployees(SESSION_ID, TOKEN));
            assertEquals("No active employees to email.", ex.getMessage());

            verify(sessionRepository, never()).markEmailed(any(), anyLong());
            verifyNoInteractions(emailSender);
        }

        // ============================================================
        // THE KEY TEST: one email per session
        // ============================================================
        @Test
        @DisplayName("second call (markEmailed returns 0) -> ALREADY_EMAILED 409, no email sent")
        void alreadyEmailed() {
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(liveSession()));
            when(employeeRepository.findByStatusIgnoreCase("ACTIVE"))
                    .thenReturn(List.of(activeEmployee()));
            when(sessionRepository.markEmailed(eq(SESSION_ID), anyLong()))
                    .thenReturn(0);          // lost the race

            assertQr(HttpStatus.CONFLICT, "ALREADY_EMAILED",
                    () -> service.sendToAllEmployees(SESSION_ID, TOKEN));

            verifyNoInteractions(emailSender);
        }

        @Test
        @DisplayName("first call: marks emailed once, hands recipients to sender, returns count")
        void successFirstTime() {
            when(sessionRepository.findById(SESSION_ID))
                    .thenReturn(Optional.of(liveSession()));
            when(employeeRepository.findByStatusIgnoreCase("ACTIVE"))
                    .thenReturn(List.of(activeEmployee()));
            when(sessionRepository.markEmailed(eq(SESSION_ID), anyLong()))
                    .thenReturn(1);          // won the race

            QrEmailResponse response = service.sendToAllEmployees(SESSION_ID, TOKEN);

            assertEquals(1, response.recipients());
            assertTrue(response.message().contains("1"));

            // exactly one markEmailed call, exactly one emailSender.broadcast call
            verify(sessionRepository).markEmailed(eq(SESSION_ID), anyLong());

            @SuppressWarnings("unchecked")
            ArgumentCaptor<List<Recipient>> recipientsCaptor =
                    ArgumentCaptor.forClass(List.class);
            verify(emailSender).broadcast(
                    recipientsCaptor.capture(),
                    eq("IT"),
                    anyString(),     // validTill
                    anyString(),     // scanUrl
                    anyString());    // qrImageUrl

            List<Recipient> sent = recipientsCaptor.getValue();
            assertEquals(1, sent.size());
            assertEquals("emp@example.com", sent.get(0).email());
        }
    }
}