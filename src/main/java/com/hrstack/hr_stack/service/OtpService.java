package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.repository.OtpRepository;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
public class OtpService {
    private final OtpRepository otpRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    public OtpService(OtpRepository otpRepository) {
        this.otpRepository = otpRepository;
    }

    public Otp createOtp(UUID adminId){
        //generate 6 digit otp
        String otpValue = String.format("%06d", secureRandom.nextInt(1_000_000));

        //current utc timestamp
        Instant createdOn = Instant.now();

        //otp expires after 5min
        Instant expiresOn = createdOn.plus(5, ChronoUnit.MINUTES);

        Otp otp = new Otp();

        otp.setOtp(otpValue);
        otp.setCreatedBy(adminId);
        otp.setCreatedOn(createdOn);
        otp.setExpiredOn(expiresOn);

        return otpRepository.save(otp);


    }

}
