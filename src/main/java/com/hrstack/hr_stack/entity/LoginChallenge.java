package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "login_challenge")
public class LoginChallenge {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "employee_id", nullable = false)
    private UUID employeeId;

    @Column(name = "otp_hash", nullable = false, length = 100)
    private String otpHash;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Long createdOn;

    @Column(name = "last_sent_on", nullable = false)
    private Long lastSentOn;

    @Column(name = "expires_on", nullable = false)
    private Long expiresOn;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "resend_count", nullable = false)
    private int resendCount;

    @Column(nullable = false)
    private boolean used;

    public UUID getId() { return id; }
    public UUID getEmployeeId() { return employeeId; }
    public void setEmployeeId(UUID employeeId) { this.employeeId = employeeId; }
    public String getOtpHash() { return otpHash; }
    public void setOtpHash(String otpHash) { this.otpHash = otpHash; }
    public Long getCreatedOn() { return createdOn; }
    public void setCreatedOn(Long createdOn) { this.createdOn = createdOn; }
    public Long getLastSentOn() { return lastSentOn; }
    public void setLastSentOn(Long lastSentOn) { this.lastSentOn = lastSentOn; }
    public Long getExpiresOn() { return expiresOn; }
    public void setExpiresOn(Long expiresOn) { this.expiresOn = expiresOn; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public int getResendCount() { return resendCount; }
    public void setResendCount(int resendCount) { this.resendCount = resendCount; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}