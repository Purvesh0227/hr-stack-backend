package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.TempFileStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import io.swagger.v3.oas.annotations.Hidden;

@RestController
@RequestMapping("/files/temp")
@SecurityRequirement(name = "bearerAuth")
public class TempFileController {

    private final TempFileStorageService tempFileStorageService;

    public TempFileController(
            TempFileStorageService tempFileStorageService) {
        this.tempFileStorageService = tempFileStorageService;
    }

    @PostMapping("/upload-url")
    @Hidden
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> generateUploadUrl(
            @RequestParam String empId,
            @RequestParam int month,
            @RequestParam int year) {

        String uploadUrl =
                tempFileStorageService.generateUploadUrl(
                        empId,
                        month,
                        year
                );

        return ResponseEntity.ok(uploadUrl);
    }
}