package com.hrstack.hr_stack.dto;

public record GoogleSignupStartResponse(
        String signupToken,
        String email,
        String firstName,
        String lastName) {}