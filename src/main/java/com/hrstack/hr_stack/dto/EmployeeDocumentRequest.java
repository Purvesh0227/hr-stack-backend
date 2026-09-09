package com.hrstack.hr_stack.dto;

public class EmployeeDocumentRequest {

    private String idProofType;
    private String idProofNumber;
    private String idProofObjectKey;

    private String addressProofType;
    private String addressProofNumber;
    private String addressProofObjectKey;

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
