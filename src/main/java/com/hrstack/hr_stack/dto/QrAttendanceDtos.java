package com.hrstack.hr_stack.dto;

import java.util.UUID;


public final class QrAttendanceDtos {

    private QrAttendanceDtos() {
    }

    public record CreateQrSessionRequest(String department) {
    }

    public record MarkQrAttendanceRequest(String token) {
    }


    public record QrSessionResponse(
            UUID sessionId,
            String token,
            String department,
            long createdOn,
            long expiresOn,
            long serverNow) {
    }

    public record QrSessionStatusResponse(
            UUID sessionId,
            String department,
            String status,
            long expiresOn,
            long serverNow,
            long scanCount) {
    }

    public record QrMarkResponse(
            long markedOn,
            String status,
            String method) {
    }
}