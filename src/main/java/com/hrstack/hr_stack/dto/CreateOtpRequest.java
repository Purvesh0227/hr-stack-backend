package com.hrstack.hr_stack.dto;

public class CreateOtpRequest {

    private Long date;

    private String department;

    public CreateOtpRequest() {
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