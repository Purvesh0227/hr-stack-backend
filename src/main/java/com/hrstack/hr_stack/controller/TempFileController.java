package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.service.TempFileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.parameters.RequestBody;
import io.swagger.v3.oas.annotations.media.Encoding;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@RestController
@RequestMapping("/files/temp")
public class TempFileController {

    private final TempFileStorageService tempFileStorageService;

    public TempFileController(
            TempFileStorageService tempFileStorageService) {
        this.tempFileStorageService = tempFileStorageService;
    }

    @SecurityRequirement(name = "bearerAuth")
    @PostMapping(
            value = "/upload",
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    public ResponseEntity<String> uploadFile(
            @RequestParam("file") MultipartFile file) {

        try {
            String objectKey =
                    tempFileStorageService.uploadTempFile(
                            file.getOriginalFilename(),
                            file.getBytes(),
                            file.getContentType()
                    );

            return ResponseEntity.ok(objectKey);

        } catch (Exception e) {
            return ResponseEntity.internalServerError()
                    .body("File upload failed");
        }
    }
}