package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "app_release")
public class AppRelease {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 50)
    private String version;

    @Column(name = "release_notes", nullable = false, columnDefinition = "TEXT")
    private String releaseNotes;

    @Column(name = "release_date", nullable = false)
    private LocalDate releaseDate;

    @Column(nullable = false, length = 20)
    private String status = "DRAFT";

    @Column(nullable = false, length = 20)
    private String platform = "ANDROID";

    @Column(name = "download_url", nullable = false, length = 500)
    private String downloadUrl;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Long createdOn;

    @Column(name = "updated_on", nullable = false)
    private Long updatedOn;

    public AppRelease() {}

    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getReleaseNotes() { return releaseNotes; }
    public void setReleaseNotes(String releaseNotes) { this.releaseNotes = releaseNotes; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }

    public String getDownloadUrl() { return downloadUrl; }
    public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }

    public Long getCreatedOn() { return createdOn; }
    public void setCreatedOn(Long createdOn) { this.createdOn = createdOn; }

    public Long getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(Long updatedOn) { this.updatedOn = updatedOn; }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }
}