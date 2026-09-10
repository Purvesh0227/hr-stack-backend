package com.hrstack.hr_stack.dto;

public class EmployeeDocumentUploadResponse {

    private String uploadUrl;
    private String objectKey;

    public EmployeeDocumentUploadResponse(String uploadUrl, String objectKey) {
        this.uploadUrl = uploadUrl;
        this.objectKey = objectKey;
    }

    public String getUploadUrl() {
        return uploadUrl;
    }    

    public void setUploadUrl(String uploadUrl) {
        this.uploadUrl = uploadUrl;
    }

    public String getObjectKey() {
        return objectKey;
    }

    public void setObjectKey(String objectKey) {
        this.objectKey = objectKey;
    }
}
