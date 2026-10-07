package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.ForgotPasswordRequest;
import com.hrstack.hr_stack.dto.VerifyResetOtpRequest;
import com.hrstack.hr_stack.service.EmailVerificationService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/employee/email-verification")
@Tag(name = "Email Verification")
public class EmailVerificationController {

    private final EmailVerificationService service;

    public EmailVerificationController(EmailVerificationService service) {
        this.service = service;
    }


    @PostMapping("/send")
    public ResponseEntity<Map<String, String>> send(
            @Valid @RequestBody ForgotPasswordRequest request) {

        service.sendOtp(request.email());
        return ResponseEntity.ok(Map.of(
                "message", "OTP sent. It is valid for 5 minutes."));
    }


    @PostMapping("/verify")
    public ResponseEntity<Map<String, String>> verify(
            @Valid @RequestBody VerifyResetOtpRequest request) {

        service.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully."));
    }

    @PostMapping("/send-link")
    public ResponseEntity<Map<String, String>> sendLink(
            @Valid @RequestBody ForgotPasswordRequest request) {

        service.sendLink(request.email());

        return ResponseEntity.ok(Map.of(
                "message", "Verification link sent. It is valid for 15 minutes."
        ));
    }


    @PostMapping("/confirm-link")
    public ResponseEntity<Map<String, String>> confirmLink(
            @RequestParam String token) {

        String email = service.confirmLink(token);

        return ResponseEntity.ok(Map.of(
                "message", "Email verified successfully.",
                "email", email
        ));
    }

    @PostMapping("/status")
    public ResponseEntity<Map<String, Object>> status(
            @Valid @RequestBody ForgotPasswordRequest request) {

        boolean verified =
                service.getVerificationStatus(request.email());

        return ResponseEntity.ok(Map.of(
                "verified", verified
        ));
    }
}