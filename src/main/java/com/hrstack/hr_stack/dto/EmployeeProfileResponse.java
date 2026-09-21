package com.hrstack.hr_stack.dto;

import com.hrstack.hr_stack.entity.EmployeeDocument;

public class EmployeeProfileResponse {

    private String id;
    private String empId;
    private String firstName;
    private String lastName;
    private String email;
    private String mobile;
    private String role;
    private String status;
    private String profilePhotoUrl;
    private Long createdOn;
    private Long updatedOn;
    private EmployeeDocument documents;

    public EmployeeProfileResponse() {
    }

    public EmployeeProfileResponse(
            String id,
            String empId,
            String firstName,
            String lastName,
            String email,
            String mobile,
            String role,
            String status,
            String profilePhotoUrl,
            Long createdOn,
            Long updatedOn,
            EmployeeDocument documents) {

        this.id = id;
        this.empId = empId;
        this.firstName = firstName;
        this.lastName = lastName;
        this.email = email;
        this.mobile = mobile;
        this.role = role;
        this.status = status;
        this.profilePhotoUrl = profilePhotoUrl;
        this.createdOn = createdOn;
        this.updatedOn = updatedOn;
        this.documents = documents;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getMobile() {
        return mobile;
    }

    public void setMobile(String mobile) {
        this.mobile = mobile;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProfilePhotoUrl() {
        return profilePhotoUrl;
    }

    public void setProfilePhotoUrl(String profilePhotoUrl) {
        this.profilePhotoUrl = profilePhotoUrl;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getUpdatedOn() {
        return updatedOn;
    }

    public void setUpdatedOn(Long updatedOn) {
        this.updatedOn = updatedOn;
    }

    public EmployeeDocument getDocuments() {
        return documents;
    }

    public void setDocuments(EmployeeDocument documents) {
        this.documents = documents;
    }
}