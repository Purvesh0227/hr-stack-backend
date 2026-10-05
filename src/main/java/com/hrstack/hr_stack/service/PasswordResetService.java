package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.PasswordResetOtp;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.PasswordResetOtpRepository;
import com.hrstack.hr_stack.security.JwtService;
import io.jsonwebtoken.Claims;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.util.UUID;

@Service
public class PasswordResetService {

    private static final long OTP_TTL_MS = 5 * 60 * 1000L;
    private static final long RESEND_COOLDOWN_MS = 60 * 1000L;
    private static final int MAX_ATTEMPTS = 5;
    private static final String INVALID_OTP = "Invalid or expired OTP.";
    private static final String INVALID_TOKEN = "Invalid or expired reset request.";

    private final PasswordResetOtpRepository otpRepository;
    private final EmployeeRepository employeeRepository;
    private final NotificationService notificationService;
    private final JwtService jwtService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    public PasswordResetService(PasswordResetOtpRepository otpRepository,
                                EmployeeRepository employeeRepository,
                                NotificationService notificationService,
                                JwtService jwtService,
                                PasswordEncoder passwordEncoder) {
        this.otpRepository = otpRepository;
        this.employeeRepository = employeeRepository;
        this.notificationService = notificationService;
        this.jwtService = jwtService;
        this.passwordEncoder = passwordEncoder;
    }

    // ---------- Phase 4: forgot password ----------
    public void requestReset(String email) {

        employeeRepository.findByEmailIgnoreCase(email.trim()).ifPresent(employee -> {
            long now = System.currentTimeMillis();

            boolean tooSoon = otpRepository
                    .findTopByEmployeeIdOrderByCreatedOnDesc(employee.getId())
                    .map(o -> now - o.getCreatedOn() < RESEND_COOLDOWN_MS)
                    .orElse(false);
            if (tooSoon) return;                       // anti email-bombing

            otpRepository.deleteByEmployeeId(employee.getId());   // invalidate old

            String otpValue = String.format("%06d", secureRandom.nextInt(1_000_000));

            PasswordResetOtp row = new PasswordResetOtp();
            row.setEmployeeId(employee.getId());
            row.setOtp(passwordEncoder.encode(otpValue));         // BCrypt hash
            row.setCreatedOn(now);
            row.setExpiresOn(now + OTP_TTL_MS);
            otpRepository.save(row);

            notificationService.sendPasswordResetOtp(employee, otpValue);
        });
    }

    // ---------- Phase 7: verify OTP -> reset token ----------
    public String verifyOtp(String email, String otp) {
        Employee employee = employeeRepository.findByEmailIgnoreCase(email.trim())
                .orElseThrow(() -> new BadRequestException(INVALID_OTP));

        PasswordResetOtp row = otpRepository
                .findTopByEmployeeIdOrderByCreatedOnDesc(employee.getId())
                .orElseThrow(() -> new BadRequestException(INVALID_OTP));

        if (row.isVerified()) {                        // OTP is single use
            throw new BadRequestException(INVALID_OTP);
        }
        if (System.currentTimeMillis() > row.getExpiresOn()) {
            otpRepository.deleteByEmployeeId(employee.getId());
            throw new BadRequestException(INVALID_OTP);
        }
        if (!passwordEncoder.matches(otp, row.getOtp())) {
            row.setAttempts(row.getAttempts() + 1);
            if (row.getAttempts() >= MAX_ATTEMPTS) {
                otpRepository.deleteByEmployeeId(employee.getId());   // lock out
            } else {
                otpRepository.save(row);
            }
            throw new BadRequestException(INVALID_OTP);
        }

        row.setVerified(true);
        otpRepository.save(row);
        return jwtService.generateResetToken(employee.getEmail(), row.getId());
    }

    // ---------- Phase 8+9: reset password ----------
    public void resetPassword(String resetToken, String newPassword) {
        Claims claims;
        try {
            claims = jwtService.parseResetToken(resetToken);
        } catch (Exception e) {
            throw new BadRequestException(INVALID_TOKEN);
        }

        // Token must still point to a VERIFIED OTP row (deleted after use => single use)
        PasswordResetOtp row = otpRepository
                .findById(UUID.fromString(claims.getId()))
                .filter(PasswordResetOtp::isVerified)
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN));

        Employee employee = employeeRepository.findById(row.getEmployeeId())
                .filter(e -> e.getEmail().equalsIgnoreCase(claims.getSubject()))
                .orElseThrow(() -> new BadRequestException(INVALID_TOKEN));

        if (passwordEncoder.matches(newPassword, employee.getPassword())) {
            throw new BadRequestException("New password must be different from the old one.");
        }

        employee.setPassword(passwordEncoder.encode(newPassword));
        employeeRepository.save(employee);

        otpRepository.deleteByEmployeeId(employee.getId());   // kill OTP + token
    }
}