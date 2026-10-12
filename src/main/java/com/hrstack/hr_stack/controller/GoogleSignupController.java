package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.GoogleSignupCompleteRequest;
import com.hrstack.hr_stack.dto.GoogleSignupStartRequest;
import com.hrstack.hr_stack.dto.GoogleSignupStartResponse;
import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.service.GoogleSignupService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
@Tag(name = "Google Sign-up")
public class GoogleSignupController {

    private final GoogleSignupService googleSignupService;

    public GoogleSignupController(GoogleSignupService googleSignupService) {
        this.googleSignupService = googleSignupService;
    }

    @PostMapping("/google-signup/start")
    public ResponseEntity<GoogleSignupStartResponse> start(
            @Valid @RequestBody GoogleSignupStartRequest request) {

        return ResponseEntity.ok(googleSignupService.start(request.idToken()));
    }

    @PostMapping("/google-signup/complete")
    public ResponseEntity<LoginResponse> complete(
            @Valid @RequestBody GoogleSignupCompleteRequest request) {

        return ResponseEntity.ok(googleSignupService.complete(request));
    }
}