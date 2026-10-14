package com.hrstack.hr_stack.dto;

import java.util.List;
import java.util.UUID;

public final class AnnouncementDtos {

    private AnnouncementDtos() {
    }

    // File already uploaded to the temp bucket through a presigned URL
    public record AttachmentInput(
            String objectKey,
            String fileName,
            String contentType,
            long size) {
    }

    // Create: newAttachments only. Update: newAttachments + removeAttachmentIds.
    public record AnnouncementRequest(
            String title,
            String message,
            String type,
            List<AttachmentInput> newAttachments,
            List<UUID> removeAttachmentIds) {
    }

    public record AttachmentResponse(
            UUID id,
            String fileName,
            String contentType,
            long size) {
    }

    public record AnnouncementResponse(
            UUID id,
            String title,
            String message,
            String type,
            String createdByName,
            long createdOn,
            Long updatedOn,
            List<AttachmentResponse> attachments) {
    }

    public record UploadUrlResponse(String uploadUrl, String objectKey) {
    }

    public record ViewUrlResponse(String url) {
    }
}