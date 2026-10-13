package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.QrAttendanceDtos.CreateQrSessionRequest;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.MarkQrAttendanceRequest;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrMarkResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionResponse;
import com.hrstack.hr_stack.dto.QrAttendanceDtos.QrSessionStatusResponse;
import com.hrstack.hr_stack.service.QrAttendanceService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee/attendance/qr")
@Tag(name = "QR Attendance")
@SecurityRequirement(name = "bearerAuth")
public class QrAttendanceController {

    private final QrAttendanceService qrAttendanceService;

    public QrAttendanceController(QrAttendanceService qrAttendanceService) {
        this.qrAttendanceService = qrAttendanceService;
    }


    @PostMapping("/sessions")
    public ResponseEntity<QrSessionResponse> createSession(
            Authentication authentication,
            @RequestBody CreateQrSessionRequest request) {

        QrSessionResponse response = qrAttendanceService.createSession(
                authentication.getName(),
                request.department());

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(response);
    }


    @GetMapping("/sessions/{id}")
    public ResponseEntity<QrSessionStatusResponse> getSessionStatus(
            @PathVariable UUID id) {

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(qrAttendanceService.getSessionStatus(id));
    }


    @DeleteMapping("/sessions/{id}")
    public ResponseEntity<Void> revokeSession(@PathVariable UUID id) {

        qrAttendanceService.revokeSession(id);
        return ResponseEntity.noContent().build();
    }


    @PostMapping("/mark")
    public ResponseEntity<QrMarkResponse> mark(
            Authentication authentication,
            @RequestBody MarkQrAttendanceRequest request) {

        return ResponseEntity.ok()
                .header("Cache-Control", "no-store")
                .body(qrAttendanceService.markAttendance(
                        authentication.getName(),
                        request.token()));
    }
}