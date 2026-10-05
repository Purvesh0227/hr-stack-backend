package com.hrstack.hr_stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequest(
        @NotBlank String resetToken,
        @NotBlank
        @Size(max = 64)   // BCrypt only reads 72 bytes
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!.*_-]).{8,}$",
                message = "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character")
        String newPassword) {}
