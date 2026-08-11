package com.hrstack.hr_stack.dto;

import com.hrstack.hr_stack.entity.Employee;

public class LoginResponse {
    private String token;
    private Employee employee;

    public LoginResponse() {
    }

    public LoginResponse(String token, Employee employee) {
        this.token = token;
        this.employee = employee;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }
}
