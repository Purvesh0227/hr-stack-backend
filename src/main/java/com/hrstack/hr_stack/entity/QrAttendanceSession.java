package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "qr_attendance_session")
public class QrAttendanceSession {

    public static final String STATUS_ACTIVE = "ACTIVE";
    public static final String STATUS_REVOKED = "REVOKED";

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    // SHA-256 hex of the QR token. Raw token is never stored.
    @Column(name = "token_hash", nullable = false, length = 64, unique = true)
    private String tokenHash;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(nullable = false)
    private String department;

    // Unix timestamp in milliseconds
    @Column(name = "created_on", nullable = false)
    private Long createdOn;

    // Unix timestamp in milliseconds
    @Column(name = "expires_on", nullable = false)
    private Long expiresOn;

    @Column(nullable = false, length = 20)
    private String status = STATUS_ACTIVE;

    // Set once when the QR is emailed to employees. NULL = not emailed yet.
    @Column(name = "emailed_on")
    private Long emailedOn;

    public QrAttendanceSession() {
    }

    public UUID getUuid() { return uuid; }
    public void setUuid(UUID uuid) { this.uuid = uuid; }

    public String getTokenHash() { return tokenHash; }
    public void setTokenHash(String tokenHash) { this.tokenHash = tokenHash; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getDepartment() { return department; }
    public void setDepartment(String department) { this.department = department; }

    public Long getCreatedOn() { return createdOn; }
    public void setCreatedOn(Long createdOn) { this.createdOn = createdOn; }

    public Long getExpiresOn() { return expiresOn; }
    public void setExpiresOn(Long expiresOn) { this.expiresOn = expiresOn; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public Long getEmailedOn() { return emailedOn; }
    public void setEmailedOn(Long emailedOn) { this.emailedOn = emailedOn; }
}
