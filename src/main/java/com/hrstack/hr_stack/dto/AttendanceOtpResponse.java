package com.hrstack.hr_stack.dto;


public class AttendanceOtpResponse {
    private String message;
    private String otp;
    private int totalEmployees;
    private int successful;
    private int failed;

    public AttendanceOtpResponse(String message, String otp, int totalEmployees, int successful, int failed) {
        this.message = message;
        this.otp = otp;
        this.totalEmployees = totalEmployees;
        this.successful = successful;
        this.failed = failed;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public int getTotalEmployees() {
        return totalEmployees;
    }

    public void setTotalEmployees(int totalEmployees) {
        this.totalEmployees = totalEmployees;
    }

    public int getSuccessful() {
        return successful;
    }

    public void setSuccessful(int successful) {
        this.successful = successful;
    }

    public int getFailed() {
        return failed;
    }

    public void setFailed(int failed) {
        this.failed = failed;
    }
}
