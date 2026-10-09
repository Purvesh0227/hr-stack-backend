package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.GoogleLoginRequest;
import com.hrstack.hr_stack.dto.LoginResponse;
import com.hrstack.hr_stack.service.GoogleAuthService;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee")
@Tag(name = "Google Login")
public class GoogleAuthController {

    private final GoogleAuthService googleAuthService;

    public GoogleAuthController(GoogleAuthService googleAuthService) {
        this.googleAuthService = googleAuthService;
    }

    @PostMapping("/google-login")
    public ResponseEntity<LoginResponse> googleLogin(
            @Valid @RequestBody GoogleLoginRequest request) {

        return ResponseEntity.ok(
                googleAuthService.login(request.idToken()));
    }
}