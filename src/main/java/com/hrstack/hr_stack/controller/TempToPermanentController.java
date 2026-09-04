package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.TempToPermanentStorageService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/files")
public class TempToPermanentController {

    private final TempToPermanentStorageService storageService;

    public TempToPermanentController(
            TempToPermanentStorageService storageService) {
        this.storageService = storageService;
    }
    @SecurityRequirement(name = "bearerAuth")
    @PostMapping("/move-to-permanent")
    public ResponseEntity<String> moveToPermanent(
            @RequestParam String tempObjectKey,
            @RequestParam String permanentObjectKey) {

        try {
            String result = storageService.moveToPermanent(
                    tempObjectKey,
                    permanentObjectKey
            );

            return ResponseEntity.ok(
                    "File moved successfully: " + result
            );

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("Failed to move file");
        }
    }
}