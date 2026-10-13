package com.hrstack.hr_stack.exception;

import org.springframework.http.HttpStatus;


public class QrAttendanceException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    public QrAttendanceException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public String getCode() {
        return code;
    }
}