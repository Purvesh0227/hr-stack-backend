package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "otp")
public class Otp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false, length = 6)
    private String otp;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    // Unix timestamp in milliseconds
    @Column(name = "created_on", nullable = false)
    private Long createdOn;

    // Unix timestamp in milliseconds
    @Column(name = "expires_on", nullable = false)
    private Long expiredOn;

    // Date for which OTP is generated
    // Stored as Unix timestamp in milliseconds
    @Column(name = "otp_date", nullable = false)
    private Long date;

    @Column(nullable = false)
    private String department;

    public Otp() {
    }

    public Otp(
            UUID uuid,
            String otp,
            UUID createdBy,
            Long createdOn,
            Long expiredOn,
            Long date,
            String department) {

        this.uuid = uuid;
        this.otp = otp;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.expiredOn = expiredOn;
        this.date = date;
        this.department = department;
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getOtp() {
        return otp;
    }

    public void setOtp(String otp) {
        this.otp = otp;
    }

    public UUID getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(UUID createdBy) {
        this.createdBy = createdBy;
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