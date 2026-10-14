package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AnnouncementDtos.*;
import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.entity.Announcement;
import com.hrstack.hr_stack.entity.AnnouncementAttachment;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AnnouncementRepository;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;

@Service
public class AnnouncementService {

    private static final Set<String> TYPES = Set.of(
            "GENERAL", "HOLIDAY", "POLICY", "EVENT", "URGENT", "MAINTENANCE"
    );

    // content type -> allowed file extension
    private static final Map<String, List<String>> ALLOWED_FILES = Map.of(
            "application/pdf", List.of(".pdf"),
            "image/png", List.of(".png"),
            "image/jpeg", List.of(".jpg", ".jpeg"),
            "image/webp", List.of(".webp")
    );

    private static final int MAX_FILES = 5;
    private static final long MAX_FILE_BYTES = 10L * 1024 * 1024;
    private static final String KEY_PREFIX = "announcements/";

    private final AnnouncementRepository announcementRepository;
    private final EmployeeRepository employeeRepository;
    private final MinioStorageService storage;

    @Value("${s3.temp-bucket}")
    private String tempBucket;

    @Value("${s3.permanent-bucket}")
    private String permanentBucket;

    public AnnouncementService(
            AnnouncementRepository announcementRepository,
            EmployeeRepository employeeRepository,
            MinioStorageService storage) {

        this.announcementRepository = announcementRepository;
        this.employeeRepository = employeeRepository;
        this.storage = storage;
    }

    // ---------- read ----------

    @Transactional(readOnly = true)
    public PageResponse<AnnouncementResponse> list(int page, int size, String type) {

        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 50),
                Sort.by(Sort.Direction.DESC, "createdOn")
        );

        Page<Announcement> result = (type == null || type.isBlank())
                ? announcementRepository.findAll(pageable)
                : announcementRepository.findByTypeIgnoreCase(type.trim(), pageable);

        return PageResponse.from(result.map(this::toResponse));
    }

    @Transactional(readOnly = true)
    public String getAttachmentViewUrl(UUID announcementId, UUID attachmentId) {

        Announcement announcement = find(announcementId);

        AnnouncementAttachment attachment = announcement.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        return storage.getSignedUrl(permanentBucket, attachment.getObjectKey());
    }

    // ---------- upload url ----------

    public UploadUrlResponse generateUploadUrl(String fileName, String contentType) {

        validateFile(fileName, contentType);

        String safeName = fileName.trim()
                .replaceAll("[^a-zA-Z0-9._-]", "_");

        if (safeName.length() > 100) {
            safeName = safeName.substring(safeName.length() - 100);
        }

        String objectKey = KEY_PREFIX + UUID.randomUUID() + "/" + safeName;

        return new UploadUrlResponse(
                storage.getPresignedUploadUrl(tempBucket, objectKey),
                objectKey
        );
    }

    // ---------- write (admin) ----------

    @Transactional
    public AnnouncementResponse create(String adminEmail, AnnouncementRequest request) {

        Employee admin = employeeRepository.findByEmail(adminEmail)
                .orElseThrow(() -> new BadRequestException("Admin not found"));

        Announcement announcement = new Announcement();

        applyFields(announcement, request);

        announcement.setCreatedBy(admin.getId());
        announcement.setCreatedByName(
                (admin.getFirstName() + " " + admin.getLastName()).trim()
        );
        announcement.setCreatedOn(System.currentTimeMillis());

        List<AttachmentInput> newFiles = safeList(request.newAttachments());
        checkFileCount(0, newFiles.size());
        attachFiles(announcement, newFiles);

        return toResponse(announcementRepository.save(announcement));
    }

    @Transactional
    public AnnouncementResponse update(UUID id, AnnouncementRequest request) {

        Announcement announcement = find(id);

        applyFields(announcement, request);
        announcement.setUpdatedOn(System.currentTimeMillis());

        Set<UUID> removeIds = new HashSet<>(safeList(request.removeAttachmentIds()));

        List<AnnouncementAttachment> removed = announcement.getAttachments().stream()
                .filter(a -> removeIds.contains(a.getId()))
                .toList();

        List<AttachmentInput> newFiles = safeList(request.newAttachments());

        checkFileCount(
                announcement.getAttachments().size() - removed.size(),
                newFiles.size()
        );

        announcement.getAttachments().removeAll(removed);
        attachFiles(announcement, newFiles);

        Announcement saved = announcementRepository.save(announcement);

        removed.forEach(a -> deleteQuietly(a.getObjectKey()));

        return toResponse(saved);
    }

    @Transactional
    public void delete(UUID id) {

        Announcement announcement = find(id);

        List<String> keys = announcement.getAttachments().stream()
                .map(AnnouncementAttachment::getObjectKey)
                .toList();

        announcementRepository.delete(announcement);

        keys.forEach(this::deleteQuietly);
    }


    private Announcement find(UUID id) {
        return announcementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Announcement not found"));
    }

    private void applyFields(Announcement announcement, AnnouncementRequest request) {

        String title = request.title() == null ? "" : request.title().trim();
        String message = request.message() == null ? "" : request.message().trim();
        String type = request.type() == null ? "" : request.type().trim().toUpperCase();

        if (title.isEmpty() || title.length() > 150) {
            throw new BadRequestException("Title is required (max 150 characters)");
        }

        if (message.isEmpty() || message.length() > 5000) {
            throw new BadRequestException("Message is required (max 5000 characters)");
        }

        if (!TYPES.contains(type)) {
            throw new BadRequestException("Invalid announcement type");
        }

        announcement.setTitle(title);
        announcement.setMessage(message);
        announcement.setType(type);
    }

    private void checkFileCount(int existing, int adding) {
        if (existing + adding > MAX_FILES) {
            throw new BadRequestException("Maximum " + MAX_FILES + " attachments allowed");
        }
    }

    private void attachFiles(Announcement announcement, List<AttachmentInput> files) {

        for (AttachmentInput file : files) {

            validateFile(file.fileName(), file.contentType());

            if (file.size() <= 0 || file.size() > MAX_FILE_BYTES) {
                throw new BadRequestException("Each file must be under 10 MB");
            }

            String key = file.objectKey();

            if (key == null || !key.startsWith(KEY_PREFIX) || key.contains("..")) {
                throw new BadRequestException("Invalid attachment");
            }

            if (!storage.exists(tempBucket, key)) {
                throw new BadRequestException(
                        "File not uploaded: " + file.fileName()
                );
            }

            storage.move(tempBucket, key, permanentBucket, key);

            AnnouncementAttachment attachment = new AnnouncementAttachment();
            attachment.setObjectKey(key);
            attachment.setFileName(file.fileName().trim());
            attachment.setContentType(file.contentType());
            attachment.setFileSize(file.size());

            announcement.addAttachment(attachment);
        }
    }

    private void validateFile(String fileName, String contentType) {

        if (fileName == null || fileName.isBlank() || contentType == null) {
            throw new BadRequestException("File name and type are required");
        }

        List<String> extensions = ALLOWED_FILES.get(contentType.toLowerCase());

        if (extensions == null) {
            throw new BadRequestException("Only PDF, PNG, JPG and WEBP files are allowed");
        }

        String lower = fileName.toLowerCase();

        if (extensions.stream().noneMatch(lower::endsWith)) {
            throw new BadRequestException("File extension does not match file type");
        }
    }

    private void deleteQuietly(String key) {
        try {
            storage.delete(permanentBucket, key);
        } catch (Exception e) {
            System.err.println("Unable to delete announcement file: " + e.getMessage());
        }
    }

    private <T> List<T> safeList(List<T> list) {
        return list == null ? List.of() : list;
    }

    private AnnouncementResponse toResponse(Announcement a) {
        return new AnnouncementResponse(
                a.getId(),
                a.getTitle(),
                a.getMessage(),
                a.getType(),
                a.getCreatedByName(),
                a.getCreatedOn(),
                a.getUpdatedOn(),
                a.getAttachments().stream()
                        .map(f -> new AttachmentResponse(
                                f.getId(),
                                f.getFileName(),
                                f.getContentType(),
                                f.getFileSize()))
                        .toList()
        );
    }
}