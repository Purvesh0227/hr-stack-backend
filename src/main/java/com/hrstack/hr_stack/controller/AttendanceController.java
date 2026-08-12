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


    // ==========================================
    // MARK OWN ATTENDANCE
    // ADMIN + EMPLOYEE
    // ==========================================

    @PostMapping("/attendance")
    public ResponseEntity<?> markAttendance(
            Authentication authentication) {

        try {

            String email = authentication.getName();

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


    // ==========================================
    // GET MY ATTENDANCE
    // ADMIN + EMPLOYEE
    // ==========================================

    @GetMapping("/attendance/my")
    public ResponseEntity<?> getMyAttendance(
            Authentication authentication) {

        try {

            String email = authentication.getName();

            List<Attendance> attendances =
                    attendanceService.getMyAttendance(email);

            return ResponseEntity.ok(attendances);

        } catch (RuntimeException e) {

            return ResponseEntity
                    .badRequest()
                    .body(Map.of(
                            "error",
                            e.getMessage()
                    ));
        }
    }


    // ==========================================
    // GET ALL ATTENDANCE
    // ADMIN ONLY
    // ==========================================

    @GetMapping("/attendance/all")
    public ResponseEntity<?> getAllAttendance(
            Authentication authentication) {

        try {

            String email = authentication.getName();

            List<Attendance> attendances =
                    attendanceService.getAllAttendance(email);

            return ResponseEntity.ok(attendances);

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