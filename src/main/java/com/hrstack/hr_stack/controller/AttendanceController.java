package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.service.AttendanceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/employee")
@CrossOrigin(origins = "http://localhost:5173")
@SecurityRequirement(name = "bearerAuth")
public class AttendanceController {

    private final AttendanceService attendanceService;

    public AttendanceController(
            AttendanceService attendanceService) {

        this.attendanceService = attendanceService;
    }

    @PostMapping("/attendance")
    public ResponseEntity<?> markAttendance(
            Authentication authentication,
            @RequestBody Map<String, String> request) {

        try {

            String email = authentication.getName();

            String enteredOtp = request.get("otp");

            if (enteredOtp == null || enteredOtp.isBlank()) {
                return ResponseEntity
                        .badRequest()
                        .body(Map.of(
                                "error",
                                "OTP is required"
                        ));
            }

            Attendance attendance =
                    attendanceService.markAttendance(
                            email,
                            enteredOtp
                    );

            return ResponseEntity.ok(attendance);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }

    @GetMapping("/attendance/view")
    public ResponseEntity<?> viewAttendance(
            Authentication authentication,
            @RequestParam(defaultValue = "MY") String scope) {

        try {
            String email = authentication.getName();
            List<Attendance> attendances =
                    attendanceService.viewAttendance(email, scope);
            return ResponseEntity.ok(attendances);
        } catch (RuntimeException e) {
            return ResponseEntity
                    .badRequest()
                    .body(Map.of("error", e.getMessage()));
        }
    }
}