package com.hrstack.hr_stack.dto;

public class OtpResponse {

    private Long createdOn;
    private Long expiredOn;
    private Long date;
    private String department;

    public OtpResponse() {
    }

    public OtpResponse(
            Long createdOn,
            Long expiredOn,
            Long date,
            String department) {

        this.createdOn = createdOn;
        this.expiredOn = expiredOn;
        this.date = date;
        this.department = department;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getExpiredOn() {
        return expiredOn;
    }

    public void setExpiredOn(Long expiredOn) {
        this.expiredOn = expiredOn;
    }

    public Long getDate() {
        return date;
    }

    public void setDate(Long date) {
        this.date = date;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }
}