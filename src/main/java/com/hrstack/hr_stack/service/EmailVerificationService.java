package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.EmailVerification;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmailVerificationRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;




@Service
public class EmailVerificationService {

    private static final long OTP_TTL_MS = 5 * 60 * 1000L;
    private static final long LINK_EXPIRY_MS = 15 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;
    private static final long VERIFIED_WINDOW_MS = 30 * 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationRepository repository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();
    private final ShortLinkService shortLinkService;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    public EmailVerificationService(EmailVerificationRepository repository,
                                    EmployeeRepository employeeRepository,
                                    NotificationService notificationService,
                                    PasswordEncoder passwordEncoder, ShortLinkService shortLinkService) {
        this.repository = repository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.passwordEncoder = passwordEncoder;
        this.shortLinkService = shortLinkService;
    }

    private String normalize(String email) {
        return email == null ? "" : email.trim().toLowerCase();
    }

//    generates token for verification
    private String generateVerificationToken() {

        byte[] tokenBytes = new byte[32];

        secureRandom.nextBytes(tokenBytes);

        return Base64.getUrlEncoder()
                .withoutPadding()
                .encodeToString(tokenBytes);
    }

//    hashing

    private String hashToken(String token) {

        try {

            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hex = new StringBuilder();

            for (byte b : hash) {
                hex.append(String.format("%02x", b));
            }
            return hex.toString();

        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available.", e);
        }
    }
    // ---------- Send OTP ----------
    public void sendOtp(String rawEmail) {
        String email = normalize(rawEmail);

        // Same rule and message the register API already uses
        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException(
                    "Email already exists. Please use another email.");
        }

        long now = System.currentTimeMillis();

        repository.findTopByEmailOrderByCreatedOnDesc(email).ifPresent(last -> {
            long waitMs = RESEND_COOLDOWN_MS - (now - last.getCreatedOn());
            if (waitMs > 0) {
                throw new BadRequestException(
                        "Please wait " + ((waitMs + 999) / 1000)
                                + " seconds before requesting a new OTP.");
            }
        });

        repository.deleteByEmail(email);                      // old OTP dies

        String otpValue = String.format("%06d", secureRandom.nextInt(1_000_000));

        EmailVerification row = new EmailVerification();
        row.setEmail(email);
        row.setOtp(passwordEncoder.encode(otpValue));         // BCrypt hash
        row.setCreatedOn(now);
        row.setExpiresOn(now + OTP_TTL_MS);
        repository.save(row);

        notificationService.sendEmailVerificationOtp(email, otpValue);
    }

    // ---------- Verify OTP ----------
    public void verifyOtp(String rawEmail, String otp) {
        String email = normalize(rawEmail);

        EmailVerification row = repository
                .findTopByEmailOrderByCreatedOnDesc(email)
                .orElseThrow(() -> new BadRequestException(
                        "No OTP found for this email. Please request a new one."));

        long now = System.currentTimeMillis();

        if (now > row.getExpiresOn()) {
            repository.deleteByEmail(email);
            throw new BadRequestException(
                    "OTP has expired. Please request a new one.");
        }

        if (row.isVerified()) {
            throw new BadRequestException(
                    "This OTP has already been used.");
        }

        if (!passwordEncoder.matches(otp, row.getOtp())) {
            int attempts = row.getAttempts() + 1;

            if (attempts >= MAX_ATTEMPTS) {
                repository.deleteByEmail(email);              // lock out
                throw new BadRequestException(
                        "Too many incorrect attempts. Please request a new OTP.");
            }

            row.setAttempts(attempts);
            repository.save(row);
            throw new BadRequestException(
                    "Incorrect OTP. " + (MAX_ATTEMPTS - attempts) + " attempt(s) left.");
        }

        row.setVerified(true);
        row.setExpiresOn(now + VERIFIED_WINDOW_MS);   // time left to finish the form
        repository.save(row);
    }

    // ---------- Confirm Verification Link ----------
    @Transactional
    public String confirmLink(String rawToken) {

        if (rawToken == null || rawToken.isBlank()) {
            throw new BadRequestException(
                    "Invalid verification token.");
        }

        String token = rawToken.trim();
        String tokenHash = hashToken(token);

        EmailVerification verification =
                repository.findByTokenHash(tokenHash)
                        .orElseThrow(() -> new BadRequestException(
                                "Invalid or expired verification link."));

        long now = System.currentTimeMillis();

        if (now > verification.getExpiresOn()) {
            repository.deleteById(verification.getId());

            throw new BadRequestException(
                    "Verification link has expired. Please request a new one.");
        }

        if (verification.isVerified()) {
            throw new BadRequestException(
                    "This verification link has already been used.");
        }

        if (employeeRepository.existsByEmailIgnoreCase(
                verification.getEmail())) {

            repository.deleteById(verification.getId());

            throw new BadRequestException(
                    "Email already exists. Please use another email.");
        }

        verification.setVerified(true);
        verification.setExpiresOn(now + VERIFIED_WINDOW_MS);

        repository.save(verification);
        return verification.getEmail();
    }

    // ---------- Used by Register ----------
    public void assertVerified(String rawEmail) {
        String email = normalize(rawEmail);

        boolean ok = repository
                .findTopByEmailOrderByCreatedOnDesc(email)
                .map(row -> row.isVerified()
                        && System.currentTimeMillis() <= row.getExpiresOn())
                .orElse(false);

        if (!ok) {
            throw new BadRequestException(
                    "Please verify your email before registering.");
        }
    }

    private void ensureEmailCanRequestVerification(String email) {

        if (employeeRepository.existsByEmailIgnoreCase(email)) {
            throw new BadRequestException(
                    "Email already exists. Please use another email.");
        }

        long now = System.currentTimeMillis();

        repository.findTopByEmailOrderByCreatedOnDesc(email).ifPresent(last -> {

            long waitMs = RESEND_COOLDOWN_MS
                    - (now - last.getCreatedOn());

            if (waitMs > 0) {
                throw new BadRequestException(
                        "Please wait " + ((waitMs + 999) / 1000)
                                + " seconds before requesting a new verification link.");
            }
        });
    }

    private String buildVerificationUrl(String token) {

        String normalizedFrontendUrl =
                frontendUrl.endsWith("/")
                        ? frontendUrl.substring(0, frontendUrl.length() - 1)
                        : frontendUrl;

        return normalizedFrontendUrl
                + "/verify-email?token="
                + token;
    }



    @Transactional
    public void sendLink(String email) {

        String normalizedEmail = email.trim().toLowerCase();

        ensureEmailCanRequestVerification(normalizedEmail);

        long now = System.currentTimeMillis();

        String token = generateVerificationToken();

        String tokenHash = hashToken(token);

        EmailVerification verification =
                new EmailVerification();

        verification.setEmail(normalizedEmail);
        verification.setOtp(null);
        verification.setTokenHash(tokenHash);
        verification.setCreatedOn(now);
        verification.setExpiresOn(
                now + LINK_EXPIRY_MS
        );
        verification.setVerified(false);
        verification.setAttempts(0);

        repository.deleteByEmail(normalizedEmail);

        repository.save(verification);

        String verificationUrl =
                buildVerificationUrl(token);

        String shortUrl =
                shortLinkService.createShortLink(
                        verificationUrl,
                        LINK_EXPIRY_MS
                );

        notificationService.sendEmailVerificationLink(
                normalizedEmail,
                shortUrl
        );
    }

    @Transactional(readOnly = true)
    public boolean getVerificationStatus(String rawEmail) {
        String email = normalize(rawEmail);

        return repository
                .findTopByEmailOrderByCreatedOnDesc(email)
                .map(row ->
                        row.isVerified()
                                && System.currentTimeMillis() <= row.getExpiresOn()
                )
                .orElse(false);
    }


    public void consume(String rawEmail) {
        repository.deleteByEmail(normalize(rawEmail));
    }
}