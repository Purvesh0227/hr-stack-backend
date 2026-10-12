
package com.hrstack.hr_stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record GoogleSignupCompleteRequest(

        @NotBlank(message = "Sign-up session is missing")
        String signupToken,

        @NotBlank(message = "First name is required")
        @Pattern(regexp = "^\\S+$", message = "First name must not contain spaces")
        String firstName,

        @NotBlank(message = "Last name is required")
        @Pattern(regexp = "^\\S+$", message = "Last name must not contain spaces")
        String lastName,

        @NotBlank(message = "Mobile number is required")
        @Pattern(regexp = "^[0-9]{10}$", message = "Mobile number must be 10 digits")
        String mobile,

        @NotBlank(message = "Password is required")
        @Pattern(
                regexp = "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[@#$%^&+=!.*_-]).{8,}$",
                message = "Password must be at least 8 characters and include an uppercase letter, a lowercase letter, a digit, and a special character"
        )
        String password,

        boolean twoFactorEnabled

) {}
