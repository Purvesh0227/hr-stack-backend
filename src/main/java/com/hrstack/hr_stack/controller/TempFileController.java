package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.TempFileStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/files/temp")
@SecurityRequirement(name = "bearerAuth")
public class TempFileController {

    private final TempFileStorageService tempFileStorageService;

    public TempFileController(
            TempFileStorageService tempFileStorageService) {
        this.tempFileStorageService = tempFileStorageService;
    }

    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam String empId,
            @RequestParam int month,
            @RequestParam int year) {

        try {

            if (file.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body("File cannot be empty");
            }

            String objectKey =
                    tempFileStorageService.uploadSalarySlipToTemp(
                            empId,
                            month,
                            year,
                            file.getBytes()
                    );

            return ResponseEntity.ok(objectKey);

        } catch (Exception e) {

            return ResponseEntity.internalServerError()
                    .body("File upload failed");
        }
    }
}