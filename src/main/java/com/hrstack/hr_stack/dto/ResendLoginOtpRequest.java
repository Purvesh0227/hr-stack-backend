package com.hrstack.hr_stack.dto;

import jakarta.validation.constraints.NotBlank;

public record ResendLoginOtpRequest(
        @NotBlank String mfaToken) {}