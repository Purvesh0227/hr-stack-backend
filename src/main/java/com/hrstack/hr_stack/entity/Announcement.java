package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;
import org.hibernate.annotations.BatchSize;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "announcement")
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, length = 150)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String message;

    @Column(nullable = false, length = 30)
    private String type;

    @Column(name = "created_by", nullable = false)
    private UUID createdBy;


    @Column(name = "created_by_name", nullable = false)
    private String createdByName;

    @Column(name = "created_on", nullable = false, updatable = false)
    private Long createdOn;

    @Column(name = "updated_on")
    private Long updatedOn;

    @OneToMany(
            mappedBy = "announcement",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @BatchSize(size = 50)
    private List<AnnouncementAttachment> attachments = new ArrayList<>();

    public UUID getId() { return id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getType() { return type; }
    public void setType(String type) { this.type = type; }

    public UUID getCreatedBy() { return createdBy; }
    public void setCreatedBy(UUID createdBy) { this.createdBy = createdBy; }

    public String getCreatedByName() { return createdByName; }
    public void setCreatedByName(String createdByName) { this.createdByName = createdByName; }

    public Long getCreatedOn() { return createdOn; }
    public void setCreatedOn(Long createdOn) { this.createdOn = createdOn; }

    public Long getUpdatedOn() { return updatedOn; }
    public void setUpdatedOn(Long updatedOn) { this.updatedOn = updatedOn; }

    public List<AnnouncementAttachment> getAttachments() { return attachments; }

    public void addAttachment(AnnouncementAttachment attachment) {
        attachment.setAnnouncement(this);
        attachments.add(attachment);
    }
}