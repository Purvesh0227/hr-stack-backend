package com.hrstack.hr_stack.dto;

public class AttendanceOtpSendStatus {
    private int totalEmployees;
    private int successful;
    private int failed;

    public AttendanceOtpSendStatus(int totalEmployees, int successful, int failed) {
        this.totalEmployees = totalEmployees;
        this.successful = successful;
        this.failed = failed;
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
