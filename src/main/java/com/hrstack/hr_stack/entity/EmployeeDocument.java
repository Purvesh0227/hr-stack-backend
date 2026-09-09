package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;

import java.util.UUID;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "employee_documents")
public class EmployeeDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @OneToOne
    @JoinColumn(
            name = "employee_id",
            nullable = false,
            unique = true
    )
    @JsonIgnore
    private Employee employee;

    private String idProofType;

    private String idProofNumber;

    private String idProofObjectKey;

    private String addressProofType;

    private String addressProofNumber;

    private String addressProofObjectKey;

    public EmployeeDocument() {
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public Employee getEmployee() {
        return employee;
    }

    public void setEmployee(Employee employee) {
        this.employee = employee;
    }

    public String getIdProofType() {
        return idProofType;
    }

    public void setIdProofType(String idProofType) {
        this.idProofType = idProofType;
    }

    public String getIdProofNumber() {
        return idProofNumber;
    }

    public void setIdProofNumber(String idProofNumber) {
        this.idProofNumber = idProofNumber;
    }

    public String getIdProofObjectKey() {
        return idProofObjectKey;
    }

    public void setIdProofObjectKey(String idProofObjectKey) {
        this.idProofObjectKey = idProofObjectKey;
    }

    public String getAddressProofType() {
        return addressProofType;
    }

    public void setAddressProofType(String addressProofType) {
        this.addressProofType = addressProofType;
    }

    public String getAddressProofNumber() {
        return addressProofNumber;
    }

    public void setAddressProofNumber(String addressProofNumber) {
        this.addressProofNumber = addressProofNumber;
    }

    public String getAddressProofObjectKey() {
        return addressProofObjectKey;
    }

    public void setAddressProofObjectKey(String addressProofObjectKey) {
        this.addressProofObjectKey = addressProofObjectKey;
    }
}