package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "otp")
public class Otp {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(nullable = false,length = 6)
    private String otp;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;

    @Column(name = "created_on",nullable = false)
    private Instant createdOn;

    @Column(name = "expires_on",nullable = false)
    private Instant expiredOn;

    public Otp() {
    }

    public Otp(UUID uuid, String otp, UUID createdBy, Instant createdOn, Instant expiredOn) {
        this.uuid = uuid;
        this.otp = otp;
        this.createdBy = createdBy;
        this.createdOn = createdOn;
        this.expiredOn = expiredOn;
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

    public Instant getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Instant createdOn) {
        this.createdOn = createdOn;
    }

    public Instant getExpiredOn() {
        return expiredOn;
    }

    public void setExpiredOn(Instant expiredOn) {
        this.expiredOn = expiredOn;
    }
}
