package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.ForgotPasswordRequest;
import com.hrstack.hr_stack.dto.ResetPasswordRequest;
import com.hrstack.hr_stack.dto.VerifyResetOtpRequest;
import com.hrstack.hr_stack.service.PasswordResetService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;


@RestController
@RequestMapping("/employee")
@Tag(name = "Password Reset")
public class PasswordResetController {

    private final PasswordResetService passwordResetService;

    public PasswordResetController(PasswordResetService passwordResetService) {
        this.passwordResetService = passwordResetService;
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Map<String, String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {

        passwordResetService.requestReset(request.email());

        return ResponseEntity.ok(Map.of("message",
                "If the account exists, a password reset OTP has been sent."));
    }

    @PostMapping("/verify-reset-otp")
    public ResponseEntity<Map<String, String>> verifyResetOtp(
            @Valid @RequestBody VerifyResetOtpRequest request) {

        String resetToken = passwordResetService.verifyOtp(request.email(), request.otp());
        return ResponseEntity.ok(Map.of("resetToken", resetToken));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Map<String, String>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {

        passwordResetService.resetPassword(request.resetToken(), request.newPassword());
        return ResponseEntity.ok(Map.of("message", "Password reset successful."));
    }
}