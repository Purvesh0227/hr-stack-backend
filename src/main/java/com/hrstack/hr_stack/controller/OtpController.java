package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Otp;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.service.OtpService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@SecurityRequirement(name = "bearerAuth")
@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "http://localhost:5173")
public class OtpController {

    private final OtpService otpService;
    private final EmployeeRepository employeeRepository;

    public OtpController(
            OtpService otpService,
            EmployeeRepository employeeRepository) {

        this.otpService = otpService;
        this.employeeRepository = employeeRepository;
    }

    @PostMapping("/createotp")
    public ResponseEntity<?> createOtp(
            Authentication authentication) {

        // Get logged-in user's email from JWT
        String email = authentication.getName();

        Employee employee = employeeRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("User not found")
                );

        // Check Admin
        if (!"ADMIN".equalsIgnoreCase(employee.getRole())) {
            return ResponseEntity
                    .status(403)
                    .body(Map.of(
                            "error",
                            "Access denied. You are not an admin."
                    ));
        }

        // Generate OTP
        Otp otp = otpService.createOtp(employee.getId());

        return ResponseEntity.ok(otp);
    }
}