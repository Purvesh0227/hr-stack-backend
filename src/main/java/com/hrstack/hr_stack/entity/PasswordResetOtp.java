package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "password_reset_otp")
public class PasswordResetOtp {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(nullable = false, length = 100)
    private String otp;

    @Column(name = "created_on", nullable = false)
    private Long createdOn;

    @Column(name = "expires_on", nullable = false)
    private Long expiresOn;

    @Column(nullable = false)
    private boolean verified;

    @Column(nullable = false)
    private int attempts;

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getOtp() { return otp; }
    public void setOtp(String otp) { this.otp = otp; }
    public Long getCreatedOn() { return createdOn; }
    public void setCreatedOn(Long createdOn) { this.createdOn = createdOn; }
    public Long getExpiresOn() { return expiresOn; }
    public void setExpiresOn(Long expiresOn) { this.expiresOn = expiresOn; }
    public boolean isVerified() { return verified; }
    public void setVerified(boolean verified) { this.verified = verified; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
}