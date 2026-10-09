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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.Optional;
import java.util.UUID;

@Service
public class LoginOtpService {

    private static final long OTP_TTL_MS = 5 * 60 * 1000L;            // OTP valid 5 min
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;        // 60 s between sends
    private static final long CHALLENGE_MAX_AGE_MS = 15 * 60 * 1000L; // whole login attempt
    private static final int MAX_ATTEMPTS = 5;                        // wrong codes per OTP
    private static final int MAX_RESENDS = 3;                         // resends per challenge

    private static final String INVALID_SESSION =
            "Login session expired. Please log in again.";

    private final LoginChallengeRepository challengeRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final AuthService authService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.two-factor.enabled:true}")
    private boolean enabled;

    public LoginOtpService(LoginChallengeRepository challengeRepository,
                           EmployeeRepository employeeRepository,
                           NotificationService notificationService,
                           JwtService jwtService,
                           AuthService authService,
                           PasswordEncoder passwordEncoder) {
        this.challengeRepository = challengeRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.authService = authService;
        this.passwordEncoder = passwordEncoder;
    }

    public boolean isEnabled() {
        return enabled;
    }

    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(LoginOtpService.class);

    // step 1: password was correct -> send OTP
    public MfaChallengeResponse start(Employee employee) {

        long now = System.currentTimeMillis();

        Optional<LoginChallenge> latest = challengeRepository
                .findTopByEmployeeIdOrderByCreatedOnDesc(employee.getId());

        if (latest.isPresent()) {
            LoginChallenge old = latest.get();
            boolean reusable = !old.isUsed()
                    && now < old.getExpiresOn()
                    && now < old.getCreatedOn() + CHALLENGE_MAX_AGE_MS
                    && now - old.getLastSentOn() < RESEND_COOLDOWN_MS;
            if (reusable) {
                return toResponse(employee, old, now);
            }
        }

        challengeRepository.deleteByEmployeeId(employee.getId());   // invalidate old

        String otp = generateOtp();

        LoginChallenge challenge = new LoginChallenge();
        challenge.setEmployeeId(employee.getId());
        challenge.setOtpHash(passwordEncoder.encode(otp));
        challenge.setCreatedOn(now);
        challenge.setLastSentOn(now);
        challenge.setExpiresOn(now + OTP_TTL_MS);
        challenge = challengeRepository.save(challenge);

        notificationService.sendLoginOtp(employee, otp);

        log.info("Login OTP sent employeeId={}", employee.getId());

        return toResponse(employee, challenge, now);
    }

    // step 2: user typed the OTP -> real login
    public LoginResponse verify(String mfaToken, String otp) {

        LoginChallenge challenge = loadChallenge(mfaToken);
        long now = System.currentTimeMillis();

        if (now > challenge.getExpiresOn()) {
            throw new BadRequestException(
                    "OTP expired. Please request a new OTP.");
        }


        if (challengeRepository.reserveAttempt(challenge.getId(), MAX_ATTEMPTS) == 0) {
            challengeRepository.deleteById(challenge.getId());
            throw new BadRequestException(
                    "Too many incorrect attempts. Please log in again.");
        }

        if (!passwordEncoder.matches(otp, challenge.getOtpHash())) {
            int attemptsLeft = MAX_ATTEMPTS - (challenge.getAttempts() + 1);
            if (attemptsLeft <= 0) {
                challengeRepository.deleteById(challenge.getId());
                throw new BadRequestException(
                        "Too many incorrect attempts. Please log in again.");
            }
            throw new BadRequestException(
                    "Invalid OTP. " + attemptsLeft + " attempt(s) left.");
        }


        if (challengeRepository.markUsed(challenge.getId()) == 0) {
            throw new BadRequestException(INVALID_SESSION);
        }

        Employee employee = employeeRepository
                .findById(challenge.getEmployeeId())
                .orElseThrow(() -> new BadRequestException(INVALID_SESSION));

        challengeRepository.deleteById(challenge.getId());

        log.info("Login OTP verified employeeId={}", employee.getId());

        return authService.issueLogin(employee);
    }

    // ---------- resend ----------
    public MfaChallengeResponse resend(String mfaToken) {

        LoginChallenge challenge = loadChallenge(mfaToken);
        long now = System.currentTimeMillis();

        long waitMs = challenge.getLastSentOn() + RESEND_COOLDOWN_MS - now;
        if (waitMs > 0) {
            throw new BadRequestException("Please wait "
                    + ((waitMs + 999) / 1000)
                    + " seconds before requesting a new OTP.");
        }

        if (challenge.getResendCount() >= MAX_RESENDS) {
            challengeRepository.deleteById(challenge.getId());
            throw new BadRequestException(
                    "Resend limit reached. Please log in again.");
        }

        Employee employee = employeeRepository
                .findById(challenge.getEmployeeId())
                .orElseThrow(() -> new BadRequestException(INVALID_SESSION));

        String otp = generateOtp();

        challenge.setOtpHash(passwordEncoder.encode(otp));   // old OTP stops working
        challenge.setLastSentOn(now);
        challenge.setExpiresOn(now + OTP_TTL_MS);
        challenge.setAttempts(0);
        challenge.setResendCount(challenge.getResendCount() + 1);
        challengeRepository.save(challenge);

        notificationService.sendLoginOtp(employee, otp);

        log.info("Login OTP sent employeeId={}", employee.getId());

        return toResponse(employee, challenge, now);
    }

    // ---------- helpers ----------
    private LoginChallenge loadChallenge(String mfaToken) {

        UUID challengeId;
        try {
            challengeId = jwtService.parseMfaToken(mfaToken);
        } catch (JwtException | IllegalArgumentException e) {
            throw new BadRequestException(INVALID_SESSION);
        }

        LoginChallenge challenge = challengeRepository
                .findById(challengeId)
                .orElseThrow(() -> new BadRequestException(INVALID_SESSION));

        long now = System.currentTimeMillis();
        if (challenge.isUsed()
                || now > challenge.getCreatedOn() + CHALLENGE_MAX_AGE_MS) {
            challengeRepository.deleteById(challengeId);
            throw new BadRequestException(INVALID_SESSION);
        }

        return challenge;
    }

    private MfaChallengeResponse toResponse(Employee employee,
                                            LoginChallenge challenge,
                                            long now) {
        long waitMs = challenge.getLastSentOn() + RESEND_COOLDOWN_MS - now;
        long waitSeconds = Math.max(0, (waitMs + 999) / 1000);

        return new MfaChallengeResponse(
                true,
                jwtService.generateMfaToken(employee.getEmail(), challenge.getId()),
                maskEmail(employee.getEmail()),
                waitSeconds);
    }

    private String generateOtp() {
        return String.format("%06d", secureRandom.nextInt(1_000_000));
    }


    private String maskEmail(String email) {
        int at = email == null ? -1 : email.indexOf('@');
        if (at <= 0) return "***";
        return email.charAt(0) + "***" + email.substring(at);
    }
}