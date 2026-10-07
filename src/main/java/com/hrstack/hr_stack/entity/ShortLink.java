package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(
        name = "short_links",
        indexes = {
                @Index(name = "idx_short_links_code", columnList = "short_code"),
                @Index(name = "idx_short_links_expires_on", columnList = "expires_on")
        }
)
public class ShortLink {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "short_code", nullable = false, unique = true, length = 20)
    private String shortCode;

    @Column(name = "target_url", nullable = false, columnDefinition = "TEXT")
    private String targetUrl;

    @Column(name = "created_on", nullable = false)
    private Long createdOn;

    @Column(name = "expires_on", nullable = false)
    private Long expiresOn;

    @Column(nullable = false)
    private boolean active = true;

    public UUID getId() {
        return id;
    }

    public String getShortCode() {
        return shortCode;
    }

    public void setShortCode(String shortCode) {
        this.shortCode = shortCode;
    }

    public String getTargetUrl() {
        return targetUrl;
    }

    public void setTargetUrl(String targetUrl) {
        this.targetUrl = targetUrl;
    }

    public Long getCreatedOn() {
        return createdOn;
    }

    public void setCreatedOn(Long createdOn) {
        this.createdOn = createdOn;
    }

    public Long getExpiresOn() {
        return expiresOn;
    }

    public void setExpiresOn(Long expiresOn) {
        this.expiresOn = expiresOn;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}