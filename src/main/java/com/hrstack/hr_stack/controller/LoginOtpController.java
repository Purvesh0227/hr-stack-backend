package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.dto.MfaChallengeResponse;
import com.hrstack.hr_stack.dto.ResendLoginOtpRequest;
import com.hrstack.hr_stack.dto.VerifyLoginOtpRequest;
import com.hrstack.hr_stack.service.LoginOtpService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
@Tag(name = "Login OTP")
public class LoginOtpController {

    private final LoginOtpService loginOtpService;

    public LoginOtpController(LoginOtpService loginOtpService) {
        this.loginOtpService = loginOtpService;
    }

    @PostMapping("/login/verify-otp")
    public ResponseEntity<LoginResponse> verifyOtp(
            @Valid @RequestBody VerifyLoginOtpRequest request) {

        return ResponseEntity.ok(
                loginOtpService.verify(request.mfaToken(), request.otp()));
    }

    @PostMapping("/login/resend-otp")
    public ResponseEntity<MfaChallengeResponse> resendOtp(
            @Valid @RequestBody ResendLoginOtpRequest request) {

        return ResponseEntity.ok(loginOtpService.resend(request.mfaToken()));
    }
}