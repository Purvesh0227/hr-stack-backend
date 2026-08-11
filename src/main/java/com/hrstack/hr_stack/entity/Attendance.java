package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;

@Entity
@Table(name = "attendance")
public class Attendance {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID uuid;

    @Column(name = "emp_id", nullable = false)
    private String empId;

    // Unix timestamp in milliseconds
    @Column(name = "marked_on", nullable = false)
    private Long markedOn;

    @Column(nullable = false)
    private String status;

    public Attendance() {
    }

    public UUID getUuid() {
        return uuid;
    }

    public void setUuid(UUID uuid) {
        this.uuid = uuid;
    }

    public String getEmpId() {
        return empId;
    }

    public void setEmpId(String empId) {
        this.empId = empId;
    }

    public Long getMarkedOn() {
        return markedOn;
    }

    public void setMarkedOn(Long markedOn) {
        this.markedOn = markedOn;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}