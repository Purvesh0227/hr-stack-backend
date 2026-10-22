package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.PasswordResetOtp;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.PasswordResetOtpRepository;
import com.hrstack.hr_stack.security.JwtService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("PasswordResetService")
class PasswordResetServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final UUID EMPLOYEE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID OTP_ROW_ID =
            UUID.fromString("66666666-6666-6666-6666-666666666666");
    private static final String RESET_TOKEN = "reset-token";
    private static final String INVALID_OTP = "Invalid or expired OTP.";
    private static final String INVALID_TOKEN = "Invalid or expired reset request.";

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;

    @Mock private PasswordResetOtpRepository otpRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private NotificationService notificationService;
    @Mock private JwtService jwtService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetService service;

    // ---------- helpers ----------

    private static Employee employee() {
        return new Employee(EMPLOYEE_ID, "Purvesh", "Test", EMAIL,
                "9999999999", "old-hash", "EMPLOYEE");
    }

    private static PasswordResetOtp row(boolean verified, long expiresInMs) {
        long now = System.currentTimeMillis();
        PasswordResetOtp r = new PasswordResetOtp();
        ReflectionTestUtils.setField(r, "id", OTP_ROW_ID);
        r.setEmployeeId(EMPLOYEE_ID);
        r.setOtp("stored-hash");
        r.setCreatedOn(now - MINUTE);
        r.setExpiresOn(now + expiresInMs);
        r.setVerified(verified);
        return r;
    }

    private static void assertBadRequest(String expectedMessage, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertEquals(expectedMessage, ex.getMessage());
    }

    // ============================================================
    // requestReset
    // ============================================================
    @Nested
    @DisplayName("requestReset")
    class RequestReset {

        @Test
        @DisplayName("unknown email -> silent no-op (no leak)")
        void unknownEmail() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.empty());

            service.requestReset(EMAIL);

            verifyNoInteractions(otpRepository, notificationService);
        }

        @Test
        @DisplayName("recent OTP (< 60s) -> no new OTP, no email")
        void resendCooldown() {
            PasswordResetOtp recent = row(false, 4 * MINUTE);
            recent.setCreatedOn(System.currentTimeMillis() - 30 * SECOND);   // 30s ago
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(recent));

            service.requestReset(EMAIL);

            verify(otpRepository, never()).save(any());
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("allowed -> deletes old row, saves BCrypt-hashed OTP, emails raw OTP")
        void success() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
            when(otpRepository.save(any(PasswordResetOtp.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.requestReset(EMAIL);

            verify(otpRepository).deleteByEmployeeId(EMPLOYEE_ID);

            ArgumentCaptor<PasswordResetOtp> savedCaptor =
                    ArgumentCaptor.forClass(PasswordResetOtp.class);
            verify(otpRepository).save(savedCaptor.capture());
            PasswordResetOtp saved = savedCaptor.getValue();
            assertEquals(EMPLOYEE_ID, saved.getEmployeeId());
            assertEquals("new-hash", saved.getOtp());
            assertEquals(5 * MINUTE, saved.getExpiresOn() - saved.getCreatedOn());
            assertTrue(!saved.isVerified());

            ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).sendPasswordResetOtp(any(Employee.class), otpCaptor.capture());
            assertTrue(otpCaptor.getValue().matches("\\d{6}"));   // 6-digit
            verify(passwordEncoder).encode(otpCaptor.getValue()); // hashed exactly that OTP
        }
    }

    // ============================================================
    // verifyOtp
    // ============================================================
    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {

        @Test
        @DisplayName("unknown email -> INVALID_OTP")
        void unknownEmail() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.empty());

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "123456"));
            verifyNoInteractions(otpRepository);
        }

        @Test
        @DisplayName("no OTP row -> INVALID_OTP")
        void noRow() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.empty());

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "123456"));
        }

        @Test
        @DisplayName("already verified OTP -> INVALID_OTP (single-use)")
        void alreadyVerified() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(row(true, 3 * MINUTE)));

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "123456"));
            verify(otpRepository, never()).save(any());
        }

        @Test
        @DisplayName("expired OTP -> INVALID_OTP and deletes row")
        void expired() {
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(row(false, -SECOND)));   // expired

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "123456"));
            verify(otpRepository).deleteByEmployeeId(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("wrong OTP under limit -> INVALID_OTP, attempts incremented")
        void wrongOtpUnderLimit() {
            PasswordResetOtp r = row(false, 3 * MINUTE);
            r.setAttempts(2);
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "000000"));
            assertEquals(3, r.getAttempts());
            verify(otpRepository).save(r);
        }

        @Test
        @DisplayName("5th wrong OTP -> INVALID_OTP and deletes row (lock out)")
        void wrongOtpLimitReached() {
            PasswordResetOtp r = row(false, 3 * MINUTE);
            r.setAttempts(4);
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

            assertBadRequest(INVALID_OTP, () -> service.verifyOtp(EMAIL, "000000"));
            verify(otpRepository).deleteByEmployeeId(EMPLOYEE_ID);
        }

        @Test
        @DisplayName("correct OTP -> returns reset token, marks row verified")
        void success() {
            PasswordResetOtp r = row(false, 3 * MINUTE);
            when(employeeRepository.findByEmailIgnoreCase(EMAIL))
                    .thenReturn(Optional.of(employee()));
            when(otpRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
            when(jwtService.generateResetToken(EMAIL, OTP_ROW_ID)).thenReturn(RESET_TOKEN);

            String token = service.verifyOtp(EMAIL, "123456");

            assertEquals(RESET_TOKEN, token);
            assertTrue(r.isVerified());
            verify(otpRepository).save(r);
        }
    }

    // ============================================================
    // resetPassword
    // ============================================================
    @Nested
    @DisplayName("resetPassword")
    class ResetPassword {

        @Test
        @DisplayName("invalid JWT -> INVALID_TOKEN")
        void badToken() {
            when(jwtService.parseResetToken(RESET_TOKEN))
                    .thenThrow(new RuntimeException("bad"));

            assertBadRequest(INVALID_TOKEN,
                    () -> service.resetPassword(RESET_TOKEN, "NewPass@123"));
        }

        @Test
        @DisplayName("row not found / not verified -> INVALID_TOKEN")
        void rowUnverified() {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                    .subject(EMAIL).id(OTP_ROW_ID.toString()).build();
            when(jwtService.parseResetToken(RESET_TOKEN)).thenReturn(claims);
            when(otpRepository.findById(OTP_ROW_ID)).thenReturn(Optional.empty());

            assertBadRequest(INVALID_TOKEN,
                    () -> service.resetPassword(RESET_TOKEN, "NewPass@123"));
        }

        @Test
        @DisplayName("new password equals old -> BadRequest and no save")
        void samePassword() {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                    .subject(EMAIL).id(OTP_ROW_ID.toString()).build();
            when(jwtService.parseResetToken(RESET_TOKEN)).thenReturn(claims);
            when(otpRepository.findById(OTP_ROW_ID)).thenReturn(Optional.of(row(true, MINUTE)));
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee()));
            when(passwordEncoder.matches("NewPass@123", "old-hash")).thenReturn(true);

            BadRequestException ex = assertThrows(BadRequestException.class,
                    () -> service.resetPassword(RESET_TOKEN, "NewPass@123"));
            assertEquals("New password must be different from the old one.", ex.getMessage());
            verify(employeeRepository, never()).save(any());
        }

        @Test
        @DisplayName("success -> hashes new password, saves employee, deletes OTP row")
        void success() {
            io.jsonwebtoken.Claims claims = io.jsonwebtoken.Jwts.claims()
                    .subject(EMAIL).id(OTP_ROW_ID.toString()).build();
            when(jwtService.parseResetToken(RESET_TOKEN)).thenReturn(claims);
            when(otpRepository.findById(OTP_ROW_ID)).thenReturn(Optional.of(row(true, MINUTE)));
            when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee()));
            when(passwordEncoder.matches("NewPass@123", "old-hash")).thenReturn(false);
            when(passwordEncoder.encode("NewPass@123")).thenReturn("new-hash");

            service.resetPassword(RESET_TOKEN, "NewPass@123");

            verify(employeeRepository).save(any(Employee.class));
            verify(otpRepository).deleteByEmployeeId(EMPLOYEE_ID);
        }
    }
}