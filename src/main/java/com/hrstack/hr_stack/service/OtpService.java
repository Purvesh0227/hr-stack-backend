package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.*;
import java.util.UUID;

@Service
public class OtpService {

    private final OtpRepository otpRepository;

    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public Otp createOtp(
            UUID adminId,
            Long requestedDate,
            String department) {

        // Generate 6 digit OTP
        String otpValue = String.format(
                "%06d",
                secureRandom.nextInt(1_000_000)
        );

        // Current Unix timestamp in milliseconds
        long createdOn = System.currentTimeMillis();

        // OTP expires after 5 minutes
        long expiredOn =
                createdOn + (5 * 60 * 1000);

        /*
         * If date is not provided,
         * use today's date at UTC midnight.
         */
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

        Otp otp = new Otp();

        otp.setOtp(otpValue);
        otp.setCreatedBy(adminId);
        otp.setCreatedOn(createdOn);
        otp.setExpiredOn(expiredOn);
        otp.setDate(otpDate);
        otp.setDepartment(department);

        return otpRepository.save(otp);
    }
}