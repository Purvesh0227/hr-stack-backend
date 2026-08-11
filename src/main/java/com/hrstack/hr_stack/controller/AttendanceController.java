package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.entity.Attendance;
import com.hrstack.hr_stack.service.AttendanceService;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

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
            Authentication authentication) {

        try {

            String email =
                    authentication.getName();

            Attendance attendance =
                    attendanceService.markAttendance(email);

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
}