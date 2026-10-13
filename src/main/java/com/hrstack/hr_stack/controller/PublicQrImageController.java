package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.QrAttendanceEmailService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;


@RestController
public class PublicQrImageController {

    private final QrAttendanceEmailService qrAttendanceEmailService;

    public PublicQrImageController(QrAttendanceEmailService qrAttendanceEmailService) {
        this.qrAttendanceEmailService = qrAttendanceEmailService;
    }

    @GetMapping("/public/attendance-qr/{token:[A-Za-z0-9_-]{20,100}}.png")
    public ResponseEntity<byte[]> qrImage(@PathVariable String token) {

        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "no-store")
                .body(qrAttendanceEmailService.renderQrImage(token));
    }
}
