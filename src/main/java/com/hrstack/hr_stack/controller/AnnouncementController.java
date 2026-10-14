package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.AnnouncementDtos.*;
import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.service.AnnouncementService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee/announcements")
@Hidden
@SecurityRequirement(name = "bearerAuth")
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    // Admin + Employee: read
    @GetMapping
    public ResponseEntity<PageResponse<AnnouncementResponse>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String type) {

        return ResponseEntity.ok(announcementService.list(page, size, type));
    }

    @GetMapping("/{id}/attachments/{attachmentId}/view-url")
    public ResponseEntity<ViewUrlResponse> viewUrl(
            @PathVariable UUID id,
            @PathVariable UUID attachmentId) {

        return ResponseEntity.ok(new ViewUrlResponse(
                announcementService.getAttachmentViewUrl(id, attachmentId)));
    }

    // Admin only: write
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/attachments/upload-url")
    public ResponseEntity<UploadUrlResponse> uploadUrl(
            @RequestParam String fileName,
            @RequestParam String contentType) {

        return ResponseEntity.ok(
                announcementService.generateUploadUrl(fileName, contentType));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ResponseEntity<AnnouncementResponse> create(
            Authentication authentication,
            @RequestBody AnnouncementRequest request) {

        return ResponseEntity.ok(
                announcementService.create(authentication.getName(), request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ResponseEntity<AnnouncementResponse> update(
            @PathVariable UUID id,
            @RequestBody AnnouncementRequest request) {

        return ResponseEntity.ok(announcementService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {

        announcementService.delete(id);

        return ResponseEntity.noContent().build();
    }
}