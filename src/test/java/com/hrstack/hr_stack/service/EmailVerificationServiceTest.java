package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.EmailVerification;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmailVerificationRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
@DisplayName("EmailVerificationService")
class EmailVerificationServiceTest {

    private static final String EMAIL = "user@example.com";
    private static final UUID ROW_ID =
            UUID.fromString("77777777-7777-7777-7777-777777777777");
    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;

    @Mock private EmailVerificationRepository repository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private NotificationService notificationService;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private ShortLinkService shortLinkService;

    @InjectMocks
    private EmailVerificationService service;

    // ---------- helpers ----------

    private void wireFrontendUrl() {
        ReflectionTestUtils.setField(service, "frontendUrl", "http://localhost:5173/");
    }

    private static EmailVerification otpRow(boolean verified, long expiresInMs) {
        long now = System.currentTimeMillis();
        EmailVerification r = new EmailVerification();
        ReflectionTestUtils.setField(r, "id", ROW_ID);
        r.setEmail(EMAIL);
        r.setOtp("stored-hash");
        r.setCreatedOn(now - MINUTE);
        r.setExpiresOn(now + expiresInMs);
        r.setVerified(verified);
        return r;
    }

    private static void assertBadRequestPrefix(String prefix, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertTrue(ex.getMessage().startsWith(prefix),
                "expected prefix '" + prefix + "' but was '" + ex.getMessage() + "'");
    }

    // ============================================================
    // sendOtp
    // ============================================================
    @Nested
    @DisplayName("sendOtp")
    class SendOtp {

        @Test
        @DisplayName("email already registered -> BadRequest")
        void alreadyRegistered() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

            assertBadRequestPrefix("Email already exists",
                    () -> service.sendOtp(EMAIL));

            verifyNoInteractions(repository, notificationService);
        }

        @Test
        @DisplayName("cooldown (< 60s since last OTP) -> BadRequest 'Please wait'")
        void cooldown() {
            EmailVerification recent = otpRow(false, 4 * MINUTE);
            recent.setCreatedOn(System.currentTimeMillis() - 20 * SECOND);

            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(recent));

            assertBadRequestPrefix("Please wait", () -> service.sendOtp(EMAIL));
            verifyNoInteractions(notificationService);
        }

        @Test
        @DisplayName("allowed -> deletes old, saves hashed OTP, emails raw OTP")
        void success() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.empty());
            when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
            when(repository.save(any(EmailVerification.class)))
                    .thenAnswer(inv -> inv.getArgument(0));

            service.sendOtp(EMAIL);

            verify(repository).deleteByEmail(EMAIL);

            ArgumentCaptor<EmailVerification> savedCaptor =
                    ArgumentCaptor.forClass(EmailVerification.class);
            verify(repository).save(savedCaptor.capture());
            EmailVerification saved = savedCaptor.getValue();
            assertEquals(EMAIL, saved.getEmail());
            assertEquals("new-hash", saved.getOtp());
            assertEquals(5 * MINUTE, saved.getExpiresOn() - saved.getCreatedOn());

            ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
            verify(notificationService).sendEmailVerificationOtp(eq(EMAIL), otpCaptor.capture());
            assertTrue(otpCaptor.getValue().matches("\\d{6}"));
            verify(passwordEncoder).encode(otpCaptor.getValue());
        }
    }

    // ============================================================
    // verifyOtp
    // ============================================================
    @Nested
    @DisplayName("verifyOtp")
    class VerifyOtp {

        @Test
        @DisplayName("no row -> BadRequest 'No OTP found'")
        void noRow() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.empty());

            assertBadRequestPrefix("No OTP found",
                    () -> service.verifyOtp(EMAIL, "123456"));
        }

        @Test
        @DisplayName("expired OTP -> BadRequest 'expired' and deletes")
        void expired() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(otpRow(false, -SECOND)));

            assertBadRequestPrefix("OTP has expired",
                    () -> service.verifyOtp(EMAIL, "123456"));
            verify(repository).deleteByEmail(EMAIL);
        }

        @Test
        @DisplayName("already used OTP -> BadRequest 'already been used'")
        void alreadyUsed() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(otpRow(true, 3 * MINUTE)));

            assertBadRequestPrefix("This OTP has already been used",
                    () -> service.verifyOtp(EMAIL, "123456"));
        }

        @Test
        @DisplayName("wrong OTP under limit -> BadRequest 'Incorrect OTP. N attempt(s) left.' and saves attempts")
        void wrongUnderLimit() {
            EmailVerification r = otpRow(false, 3 * MINUTE);
            r.setAttempts(2);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

            assertBadRequestPrefix("Incorrect OTP. 2 attempt(s) left.",
                    () -> service.verifyOtp(EMAIL, "000000"));

            assertEquals(3, r.getAttempts());
            verify(repository).save(r);
        }

        @Test
        @DisplayName("5th wrong OTP -> BadRequest 'Too many incorrect attempts' and deletes row")
        void wrongLimitReached() {
            EmailVerification r = otpRow(false, 3 * MINUTE);
            r.setAttempts(4);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

            assertBadRequestPrefix("Too many incorrect attempts",
                    () -> service.verifyOtp(EMAIL, "000000"));

            verify(repository).deleteByEmail(EMAIL);
        }

        @Test
        @DisplayName("correct OTP -> marks verified, extends expiry to 30 min")
        void success() {
            EmailVerification r = otpRow(false, 3 * MINUTE);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(r));
            when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);

            service.verifyOtp(EMAIL, "123456");

            assertTrue(r.isVerified());
            long windowMs = r.getExpiresOn() - System.currentTimeMillis();
            assertTrue(windowMs > 29 * MINUTE && windowMs <= 30 * MINUTE + 2 * SECOND,
                    "expected ~30 minutes verified window, was " + windowMs + "ms");
            verify(repository).save(r);
        }
    }

    // ============================================================
    // sendLink
    // ============================================================
    @Nested
    @DisplayName("sendLink")
    class SendLink {

        @Test
        @DisplayName("allowed -> saves hashed token, emails shortlink")
        void success() {
            wireFrontendUrl();
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(false);
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.empty());
            when(repository.save(any(EmailVerification.class)))
                    .thenAnswer(inv -> {
                        EmailVerification saved = inv.getArgument(0);
                        ReflectionTestUtils.setField(saved, "id", ROW_ID);
                        return saved;
                    });
            when(shortLinkService.createShortLink(anyString(), anyLong()))
                    .thenReturn("http://localhost:8080/s/abcd1234");

            service.sendLink(EMAIL);

            ArgumentCaptor<EmailVerification> savedCaptor =
                    ArgumentCaptor.forClass(EmailVerification.class);
            verify(repository).save(savedCaptor.capture());
            EmailVerification saved = savedCaptor.getValue();
            assertEquals(EMAIL, saved.getEmail());
            assertNotNull(saved.getTokenHash());
            assertEquals(64, saved.getTokenHash().length());   // 32 bytes -> SHA-256 hex
            assertFalse(saved.isVerified());
            assertEquals(15 * MINUTE, saved.getExpiresOn() - saved.getCreatedOn());

            verify(shortLinkService).createShortLink(
                    anyString(), eq(15 * MINUTE));
            verify(notificationService).sendEmailVerificationLink(
                    eq(EMAIL), eq("http://localhost:8080/s/abcd1234"));
        }

        @Test
        @DisplayName("email already registered -> BadRequest")
        void alreadyRegistered() {
            when(employeeRepository.existsByEmailIgnoreCase(EMAIL)).thenReturn(true);

            assertBadRequestPrefix("Email already exists",
                    () -> service.sendLink(EMAIL));

            verifyNoInteractions(shortLinkService, notificationService);
        }
    }

    // ============================================================
    // assertVerified  (the gate used by register)
    // ============================================================
    @Nested
    @DisplayName("assertVerified")
    class AssertVerified {

        @Test
        @DisplayName("no row -> BadRequest 'Please verify your email before registering.'")
        void noRow() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.empty());

            assertBadRequestPrefix("Please verify your email",
                    () -> service.assertVerified(EMAIL));
        }

        @Test
        @DisplayName("row exists but not verified -> BadRequest")
        void notVerified() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(otpRow(false, 3 * MINUTE)));

            assertBadRequestPrefix("Please verify your email",
                    () -> service.assertVerified(EMAIL));
        }

        @Test
        @DisplayName("verified but expired window -> BadRequest")
        void verifiedButExpired() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(otpRow(true, -SECOND)));

            assertBadRequestPrefix("Please verify your email",
                    () -> service.assertVerified(EMAIL));
        }

        @Test
        @DisplayName("verified and within window -> no exception")
        void success() {
            when(repository.findTopByEmailOrderByCreatedOnDesc(EMAIL))
                    .thenReturn(Optional.of(otpRow(true, 5 * MINUTE)));

            service.assertVerified(EMAIL);   // no throw
        }
    }
}