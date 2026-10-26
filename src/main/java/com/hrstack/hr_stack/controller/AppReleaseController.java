package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.AppReleaseDtos.AppReleaseResponse;
import com.hrstack.hr_stack.dto.AppReleaseDtos.CreateAppReleaseRequest;
import com.hrstack.hr_stack.service.AppReleaseService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/employee/app-releases")
@Tag(name = "App Releases")
@SecurityRequirement(name = "bearerAuth")
public class AppReleaseController {

    private final AppReleaseService service;

    public AppReleaseController(AppReleaseService service) {
        this.service = service;
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<AppReleaseResponse>> getAdminReleases() {
        return ResponseEntity.ok(service.getAllForAdmin());
    }

    @GetMapping("/public")
    public ResponseEntity<List<AppReleaseResponse>> getPublicReleases() {
        return ResponseEntity.ok(service.getReleasedForEmployee());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppReleaseResponse> createRelease(@RequestBody CreateAppReleaseRequest request) {
        return ResponseEntity.ok(service.createRelease(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<AppReleaseResponse> updateRelease(
            @PathVariable UUID id,
            @RequestBody CreateAppReleaseRequest request) {
        return ResponseEntity.ok(service.updateRelease(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> deleteRelease(@PathVariable UUID id) {
        service.deleteRelease(id);
        return ResponseEntity.noContent().build();
    }
}