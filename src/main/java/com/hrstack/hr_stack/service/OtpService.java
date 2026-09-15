package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AttendanceOtpSendStatus;
import com.hrstack.hr_stack.dto.OtpResponse;
import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.OtpRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Optional;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpRepository otpRepository;
    private final NotificationService notificationService;
    private final EmployeeRepository employeeRepository;
    private final SecureRandom secureRandom = new SecureRandom();
    private final PasswordEncoder passwordEncoder;

    public OtpService(
            OtpRepository otpRepository,
            NotificationService notificationService,
            EmployeeRepository employeeRepository,
            PasswordEncoder passwordEncoder) {

        this.otpRepository = otpRepository;
        this.notificationService = notificationService;
        this.employeeRepository = employeeRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public OtpResponse createOtp(
            UUID adminId,
            Long requestedDate,
            String department) {

        long otpDate;

        if (requestedDate == null) {

            LocalDate today =
                    LocalDate.now(ZoneOffset.UTC);

            otpDate = today
                    .atStartOfDay(ZoneOffset.UTC)
                    .toInstant()
                    .toEpochMilli();

        } else {

            otpDate = requestedDate;
        }

        // Backend 5-minute OTP cooldown
        Optional<Otp> latestOtp =
                otpRepository.findTopByDateAndDepartmentOrderByCreatedOnDesc(
                        otpDate,
                        department
                );

        if (latestOtp.isPresent()) {

            long elapsedTime =
                    System.currentTimeMillis()
                            - latestOtp.get().getCreatedOn();

            long cooldownTime =
                    5 * 60 * 1000;

            if (elapsedTime < cooldownTime) {

                long remainingSeconds =
                        (cooldownTime - elapsedTime) / 1000;

                throw new BadRequestException(
                        "OTP can be generated again after "
                                + remainingSeconds
                                + " seconds."
                );
            }
        }

        // Generate 6 digit OTP
        String otpValue = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // Current Unix timestamp in milliseconds
        long createdOn =
                System.currentTimeMillis();

        // OTP expires after 5 minutes
        long expiredOn =
                createdOn + (5 * 60 * 1000);

        Otp otp = new Otp();

        // Store hashed OTP in database
        otp.setOtp(
                passwordEncoder.encode(otpValue)
        );

        otp.setCreatedBy(adminId);
        otp.setCreatedOn(createdOn);
        otp.setExpiredOn(expiredOn);
        otp.setDate(otpDate);
        otp.setDepartment(department);

        // Save OTP
        Otp savedOtp =
                otpRepository.save(otp);

        // Send original OTP to active employees
        AttendanceOtpSendStatus sendStatus =
                notificationService.broadcastAttendanceOtp(
                        otpValue
                );

        // Return original OTP to authorized admin frontend
        return new OtpResponse(
                savedOtp.getCreatedOn(),
                savedOtp.getExpiredOn(),
                savedOtp.getDate(),
                savedOtp.getDepartment()
        );
    }
}