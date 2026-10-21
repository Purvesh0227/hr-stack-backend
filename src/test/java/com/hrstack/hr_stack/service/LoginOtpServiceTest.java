package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.dto.MfaChallengeResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.LoginChallenge;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.LoginChallengeRepository;
import com.hrstack.hr_stack.security.JwtService;
import io.jsonwebtoken.JwtException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.function.Executable;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("LoginOtpService")
class LoginOtpServiceTest {

    private static final UUID EMPLOYEE_ID =
            UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID CHALLENGE_ID =
            UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final String EMAIL = "user@example.com";
    private static final String MFA_TOKEN = "mfa-token";
    private static final String SESSION_EXPIRED =
            "Login session expired. Please log in again.";

    private static final long SECOND = 1000L;
    private static final long MINUTE = 60 * SECOND;

    @Mock private LoginChallengeRepository challengeRepository;
    @Mock private EmployeeRepository employeeRepository;
    @Mock private NotificationService notificationService;
    @Mock private JwtService jwtService;
    @Mock private AuthService authService;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private LoginOtpService service;

    // ---------- helpers ----------

    private static Employee employee() {
        return new Employee(EMPLOYEE_ID, "Purvesh", "Test", EMAIL,
                "9999999999", "password", "EMPLOYEE");
    }

    /** Valid challenge: created 1 min ago, OTP valid for 4 more minutes, not used, 0 attempts. */
    private static LoginChallenge challenge() {
        long now = System.currentTimeMillis();
        LoginChallenge c = new LoginChallenge();
        ReflectionTestUtils.setField(c, "id", CHALLENGE_ID);   // id has no setter
        c.setEmployeeId(EMPLOYEE_ID);
        c.setOtpHash("stored-hash");
        c.setCreatedOn(now - MINUTE);
        c.setLastSentOn(now - MINUTE);
        c.setExpiresOn(now + 4 * MINUTE);
        return c;
    }

    private void givenChallenge(LoginChallenge challenge) {
        when(jwtService.parseMfaToken(MFA_TOKEN)).thenReturn(CHALLENGE_ID);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.of(challenge));
    }

    private void givenNewChallengeCanBeSaved() {
        when(passwordEncoder.encode(anyString())).thenReturn("encoded-otp");
        when(challengeRepository.save(any(LoginChallenge.class))).thenAnswer(invocation -> {
            LoginChallenge saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", CHALLENGE_ID);
            return saved;
        });
        when(jwtService.generateMfaToken(EMAIL, CHALLENGE_ID)).thenReturn(MFA_TOKEN);
    }

    private static void assertBadRequest(String expectedMessage, Executable call) {
        BadRequestException ex = assertThrows(BadRequestException.class, call);
        assertEquals(expectedMessage, ex.getMessage());
    }

    // ---------- start ----------

    @Test
    void start_whenNoPreviousChallenge_savesHashedOtpAndEmailsTheSameOtp() {
        when(challengeRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                .thenReturn(Optional.empty());
        givenNewChallengeCanBeSaved();

        MfaChallengeResponse response = service.start(employee());

        assertTrue(response.mfaRequired());
        assertEquals(MFA_TOKEN, response.mfaToken());
        assertEquals("u***@example.com", response.maskedEmail());
        assertEquals(60, response.resendAvailableInSeconds());

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendLoginOtp(any(Employee.class), otpCaptor.capture());
        assertTrue(otpCaptor.getValue().matches("\\d{6}"));
        verify(passwordEncoder).encode(otpCaptor.getValue());   // hash is of the emailed code

        ArgumentCaptor<LoginChallenge> savedCaptor = ArgumentCaptor.forClass(LoginChallenge.class);
        verify(challengeRepository).save(savedCaptor.capture());
        LoginChallenge saved = savedCaptor.getValue();
        assertEquals(EMPLOYEE_ID, saved.getEmployeeId());
        assertEquals("encoded-otp", saved.getOtpHash());
        assertEquals(0, saved.getAttempts());
        assertEquals(0, saved.getResendCount());
        assertEquals(5 * MINUTE, saved.getExpiresOn() - saved.getCreatedOn());
        verify(challengeRepository).deleteByEmployeeId(EMPLOYEE_ID);
    }

    @Test
    void start_whenOtpWasSentLessThanOneMinuteAgo_reusesChallengeAndDoesNotEmailAgain() {
        LoginChallenge existing = challenge();
        existing.setLastSentOn(System.currentTimeMillis() - 10 * SECOND);
        when(challengeRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                .thenReturn(Optional.of(existing));
        when(jwtService.generateMfaToken(EMAIL, CHALLENGE_ID)).thenReturn(MFA_TOKEN);

        MfaChallengeResponse response = service.start(employee());

        assertEquals(MFA_TOKEN, response.mfaToken());
        assertTrue(response.resendAvailableInSeconds() >= 49
                && response.resendAvailableInSeconds() <= 50);
        verifyNoInteractions(notificationService);
        verify(challengeRepository, never()).save(any());
        verify(challengeRepository, never()).deleteByEmployeeId(any());
    }

    @Test
    void start_whenLastOtpIsOlderThanOneMinute_deletesOldChallengeAndSendsNewOtp() {
        LoginChallenge old = challenge();
        old.setLastSentOn(System.currentTimeMillis() - 61 * SECOND);
        when(challengeRepository.findTopByEmployeeIdOrderByCreatedOnDesc(EMPLOYEE_ID))
                .thenReturn(Optional.of(old));
        givenNewChallengeCanBeSaved();

        service.start(employee());

        verify(challengeRepository).deleteByEmployeeId(EMPLOYEE_ID);
        verify(notificationService).sendLoginOtp(any(Employee.class), anyString());
    }

    // ---------- verify ----------

    @Test
    void verify_whenOtpIsCorrect_issuesLoginAndDeletesChallenge() {
        givenChallenge(challenge());
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(1);
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
        when(challengeRepository.markUsed(CHALLENGE_ID)).thenReturn(1);
        Employee employee = employee();
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee));
        LoginResponse loginResponse = mock(LoginResponse.class);
        when(authService.issueLogin(employee)).thenReturn(loginResponse);

        LoginResponse result = service.verify(MFA_TOKEN, "123456");

        assertSame(loginResponse, result);
        verify(challengeRepository).deleteById(CHALLENGE_ID);
    }

    @ParameterizedTest(name = "{0} earlier wrong attempts -> {1} attempt(s) left")
    @CsvSource({"0, 4", "2, 2", "3, 1"})
    void verify_whenOtpIsWrong_reportsAttemptsLeft(int earlierAttempts, int expectedLeft) {
        LoginChallenge c = challenge();
        c.setAttempts(earlierAttempts);
        givenChallenge(c);
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(1);
        when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

        assertBadRequest("Invalid OTP. " + expectedLeft + " attempt(s) left.",
                () -> service.verify(MFA_TOKEN, "000000"));

        verify(challengeRepository, never()).markUsed(any());
        verifyNoInteractions(authService);
    }

    @Test
    void verify_whenFifthWrongAttempt_deletesChallengeAndAsksToLoginAgain() {
        LoginChallenge c = challenge();
        c.setAttempts(4);
        givenChallenge(c);
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(1);
        when(passwordEncoder.matches("000000", "stored-hash")).thenReturn(false);

        assertBadRequest("Too many incorrect attempts. Please log in again.",
                () -> service.verify(MFA_TOKEN, "000000"));

        verify(challengeRepository).deleteById(CHALLENGE_ID);
    }

    @Test
    void verify_whenNoAttemptsLeft_deletesChallengeWithoutCheckingOtp() {
        givenChallenge(challenge());
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(0);

        assertBadRequest("Too many incorrect attempts. Please log in again.",
                () -> service.verify(MFA_TOKEN, "123456"));

        verify(challengeRepository).deleteById(CHALLENGE_ID);
        verifyNoInteractions(passwordEncoder);
    }

    @Test
    void verify_whenOtpExpired_throwsExpiredAndDoesNotUseAnAttempt() {
        LoginChallenge c = challenge();
        c.setExpiresOn(System.currentTimeMillis() - SECOND);
        givenChallenge(c);

        assertBadRequest("OTP expired. Please request a new OTP.",
                () -> service.verify(MFA_TOKEN, "123456"));

        verify(challengeRepository, never()).reserveAttempt(any(), anyInt());
    }

    @Test
    void verify_whenChallengeAlreadyUsed_throwsSessionExpiredAndDeletesIt() {
        LoginChallenge c = challenge();
        c.setUsed(true);
        givenChallenge(c);

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));

        verify(challengeRepository).deleteById(CHALLENGE_ID);
    }

    @Test
    void verify_whenLoginSessionOlderThanFifteenMinutes_throwsSessionExpiredAndDeletesIt() {
        LoginChallenge c = challenge();
        c.setCreatedOn(System.currentTimeMillis() - 16 * MINUTE);
        givenChallenge(c);

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));

        verify(challengeRepository).deleteById(CHALLENGE_ID);
    }

    @ParameterizedTest(name = "token error {0}")
    @ValueSource(classes = {JwtException.class, IllegalArgumentException.class})
    void verify_whenTokenIsInvalid_throwsSessionExpired(Class<? extends Throwable> error) {
        when(jwtService.parseMfaToken(MFA_TOKEN)).thenThrow(error);

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));

        verifyNoInteractions(challengeRepository);
    }

    @Test
    void verify_whenChallengeNotFound_throwsSessionExpired() {
        when(jwtService.parseMfaToken(MFA_TOKEN)).thenReturn(CHALLENGE_ID);
        when(challengeRepository.findById(CHALLENGE_ID)).thenReturn(Optional.empty());

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));
    }

    @Test
    void verify_whenChallengeWasUsedByAnotherRequest_throwsSessionExpired() {
        givenChallenge(challenge());
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(1);
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
        when(challengeRepository.markUsed(CHALLENGE_ID)).thenReturn(0);   // lost the race

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));

        verifyNoInteractions(authService);
    }

    @Test
    void verify_whenEmployeeNoLongerExists_throwsSessionExpired() {
        givenChallenge(challenge());
        when(challengeRepository.reserveAttempt(CHALLENGE_ID, 5)).thenReturn(1);
        when(passwordEncoder.matches("123456", "stored-hash")).thenReturn(true);
        when(challengeRepository.markUsed(CHALLENGE_ID)).thenReturn(1);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertBadRequest(SESSION_EXPIRED, () -> service.verify(MFA_TOKEN, "123456"));

        verifyNoInteractions(authService);
    }

    // ---------- resend ----------

    @Test
    void resend_whenCooldownNotOver_throwsWaitMessageAndSendsNothing() {
        LoginChallenge c = challenge();
        c.setLastSentOn(System.currentTimeMillis() - 30 * SECOND);
        givenChallenge(c);

        BadRequestException ex = assertThrows(BadRequestException.class,
                () -> service.resend(MFA_TOKEN));

        assertTrue(ex.getMessage().startsWith("Please wait "));
        verifyNoInteractions(notificationService);
        verify(challengeRepository, never()).save(any());
    }

    @Test
    void resend_whenResendLimitReached_deletesChallengeAndAsksToLoginAgain() {
        LoginChallenge c = challenge();
        c.setResendCount(3);
        givenChallenge(c);

        assertBadRequest("Resend limit reached. Please log in again.",
                () -> service.resend(MFA_TOKEN));

        verify(challengeRepository).deleteById(CHALLENGE_ID);
        verifyNoInteractions(notificationService);
    }

    @Test
    void resend_whenAllowed_sendsNewOtpAndResetsAttempts() {
        LoginChallenge c = challenge();
        c.setAttempts(3);
        c.setResendCount(1);
        givenChallenge(c);
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.of(employee()));
        when(passwordEncoder.encode(anyString())).thenReturn("new-hash");
        when(jwtService.generateMfaToken(EMAIL, CHALLENGE_ID)).thenReturn(MFA_TOKEN);

        MfaChallengeResponse response = service.resend(MFA_TOKEN);

        assertEquals("new-hash", c.getOtpHash());               // old OTP stops working
        assertEquals(0, c.getAttempts());
        assertEquals(2, c.getResendCount());
        assertEquals(5 * MINUTE, c.getExpiresOn() - c.getLastSentOn());
        assertEquals(60, response.resendAvailableInSeconds());
        verify(challengeRepository).save(c);

        ArgumentCaptor<String> otpCaptor = ArgumentCaptor.forClass(String.class);
        verify(notificationService).sendLoginOtp(any(Employee.class), otpCaptor.capture());
        assertTrue(otpCaptor.getValue().matches("\\d{6}"));
    }

    @Test
    void resend_whenEmployeeNoLongerExists_throwsSessionExpired() {
        givenChallenge(challenge());
        when(employeeRepository.findById(EMPLOYEE_ID)).thenReturn(Optional.empty());

        assertBadRequest(SESSION_EXPIRED, () -> service.resend(MFA_TOKEN));

        verifyNoInteractions(notificationService);
    }

    @Test
    void resend_whenTokenIsInvalid_throwsSessionExpired() {
        when(jwtService.parseMfaToken(MFA_TOKEN)).thenThrow(new JwtException("bad token"));

        assertBadRequest(SESSION_EXPIRED, () -> service.resend(MFA_TOKEN));

        verifyNoInteractions(challengeRepository);
    }
}