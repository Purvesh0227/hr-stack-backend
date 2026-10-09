package com.hrstack.hr_stack.dto;

public record MfaChallengeResponse(
        boolean mfaRequired,
        String mfaToken,
        String maskedEmail,
        long resendAvailableInSeconds) {}