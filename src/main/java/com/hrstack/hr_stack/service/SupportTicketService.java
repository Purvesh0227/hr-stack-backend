package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.SupportTicketDtos.*;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.SupportTicket;
import com.hrstack.hr_stack.entity.SupportTicketAttachment;
import com.hrstack.hr_stack.entity.SupportTicketStatusHistory;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;
    private final MinioStorageService storage;

    @Value("${app.support.email}")
    private String supportEmail;

    @Value("${s3.temp-bucket}")
    private String tempBucket;

    @Value("${s3.permanent-bucket}")
    private String permanentBucket;

    private static final String KEY_PREFIX = "support-tickets/";
    private static final int MAX_FILES = 3;
    private static final long MAX_FILE_BYTES = 5L * 1024 * 1024;

    private static final List<String> ALLOWED_TYPES = List.of(
            "image/png",
            "image/jpeg",
            "application/pdf",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "video/mp4"
    );

    public SupportTicketService(
            SupportTicketRepository ticketRepository,
            EmployeeRepository employeeRepository,
            EmailService emailService,
            InAppNotificationService inAppNotificationService,
            MinioStorageService storage) {
        this.ticketRepository = ticketRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
        this.storage = storage;
    }

    public UploadUrlResponse generateUploadUrl(String fileName, String contentType) {
        if (contentType == null || !ALLOWED_TYPES.contains(contentType.toLowerCase())) {
            throw new BadRequestException(
                    "Invalid file format. Only PDF, DOCX, PNG, JPG, or MP4 are allowed. If you need to send other formats, please email support@hrstack.com."
            );
        }

        String safeName = fileName.trim().replaceAll("[^a-zA-Z0-9._-]", "_");
        String objectKey = KEY_PREFIX + UUID.randomUUID() + "/" + safeName;

        return new UploadUrlResponse(
                storage.getPresignedUploadUrl(tempBucket, objectKey),
                objectKey
        );
    }

    @Transactional
    public void createTicket(String userEmail, CreateTicketRequest request) {
        Employee employee = employeeRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        SupportTicket ticket = new SupportTicket();
        ticket.setEmployeeId(employee.getId());
        ticket.setEmployeeName(employee.getFirstName() + " " + employee.getLastName());
        ticket.setEmployeeEmail(employee.getEmail());
        ticket.setEmployeeMobile(employee.getMobile());
        ticket.setSubject(request.subject());
        ticket.setMessage(request.message());
        ticket.setPriority(request.priority());
        ticket.setStatus("OPEN");
        ticket.setCreatedOn(System.currentTimeMillis());

        SupportTicketStatusHistory history = new SupportTicketStatusHistory();
        history.setTicket(ticket);
        history.setStatus("OPEN");
        history.setChangedOn(System.currentTimeMillis());
        ticket.getStatusHistory().add(history);

        List<AttachmentInput> files = request.attachments();
        if (files != null) {
            if (files.size() > MAX_FILES) {
                throw new BadRequestException(
                        "Maximum " + MAX_FILES + " attachments allowed. If you need to send more files, please email support@hrstack.com."
                );
            }

            for (AttachmentInput file : files) {
                if (file.size() <= 0 || file.size() > MAX_FILE_BYTES) {
                    throw new BadRequestException(
                            "File is too large. Each file must be under 5 MB. For larger files, please email support@hrstack.com."
                    );
                }

                if (file.contentType() == null || !ALLOWED_TYPES.contains(file.contentType().toLowerCase())) {
                    throw new BadRequestException(
                            "Invalid file format. Only PDF, DOCX, PNG, JPG, or MP4 are allowed. If you need to send other formats, please email support@hrstack.com."
                    );
                }

                String key = file.objectKey();
                if (key == null || !key.startsWith(KEY_PREFIX) || key.contains("..")) {
                    throw new BadRequestException("Invalid attachment path.");
                }

                if (!storage.exists(tempBucket, key)) {
                    throw new BadRequestException("File not uploaded: " + file.fileName());
                }

                storage.move(tempBucket, key, permanentBucket, key);

                SupportTicketAttachment attachment = new SupportTicketAttachment();
                attachment.setTicket(ticket);
                attachment.setObjectKey(key);
                attachment.setFileName(file.fileName());
                attachment.setContentType(file.contentType());
                attachment.setFileSize(file.size());
                ticket.getAttachments().add(attachment);
            }
        }

        SupportTicket savedTicket = ticketRepository.save(ticket);
        sendSupportEmail(savedTicket);
    }

    private void sendSupportEmail(SupportTicket ticket) {
        try {
            String htmlBody = emailService.loadTemplate("templates/email/support-ticket.html");

            String priorityColor = switch (ticket.getPriority()) {
                case "URGENT" -> "#dc2626";
                case "HIGH" -> "#ea580c";
                case "MEDIUM" -> "#ca8a04";
                default -> "#16a34a";
            };

            htmlBody = htmlBody
                    .replace("{{employeeName}}", ticket.getEmployeeName())
                    .replace("{{employeeEmail}}", ticket.getEmployeeEmail())
                    .replace("{{employeeMobile}}", ticket.getEmployeeMobile())
                    .replace("{{priority}}", ticket.getPriority())
                    .replace("{{priorityColor}}", priorityColor)
                    .replace("{{subject}}", ticket.getSubject())
                    .replace("{{message}}", ticket.getMessage().replace("\n", "<br>"));

            emailService.sendHtmlEmail(
                    supportEmail,
                    "New Support Ticket: " + ticket.getSubject(),
                    htmlBody
            );
        } catch (Exception e) {
            System.err.println("Failed to send support ticket email: " + e.getMessage());
        }
    }

    public Page<TicketResponse> getTickets(String status, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets;

        if (status == null || status.isBlank() || "ALL".equalsIgnoreCase(status)) {
            tickets = ticketRepository.findAllByOrderByCreatedOnDesc(pageable);
        } else {
            tickets = ticketRepository.findByStatusOrderByCreatedOnDesc(status.toUpperCase(), pageable);
        }

        return tickets.map(this::mapToResponse);
    }

    public Page<TicketResponse> getMyTickets(String email, int page, int size) {
        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));

        Pageable pageable = PageRequest.of(page, size);
        Page<SupportTicket> tickets = ticketRepository.findByEmployeeIdOrderByCreatedOnDesc(employee.getId(), pageable);

        return tickets.map(this::mapToResponse);
    }

    @Transactional
    public void updateTicketStatus(UUID ticketId, String newStatus) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        ticket.setStatus(newStatus.toUpperCase());

        SupportTicketStatusHistory history = new SupportTicketStatusHistory();
        history.setTicket(ticket);
        history.setStatus(newStatus.toUpperCase());
        history.setChangedOn(System.currentTimeMillis());
        ticket.getStatusHistory().add(history);

        if ("RESOLVED".equalsIgnoreCase(newStatus)) {
            ticket.setResolvedOn(System.currentTimeMillis());
            inAppNotificationService.createNotification(
                    ticket.getEmployeeId(),
                    "SUPPORT_TICKET",
                    "Your support ticket '" + ticket.getSubject() + "' has been marked as RESOLVED."
            );
        } else {
            ticket.setResolvedOn(null);
        }

        ticketRepository.save(ticket);
    }

    private TicketResponse mapToResponse(SupportTicket t) {
        List<AttachmentResponse> attachments = t.getAttachments().stream()
                .map(a -> new AttachmentResponse(a.getId(), a.getFileName(), a.getContentType(), a.getFileSize()))
                .toList();

        List<StatusHistoryResponse> history = t.getStatusHistory().stream()
                .sorted((a, b) -> a.getChangedOn().compareTo(b.getChangedOn()))
                .map(h -> new StatusHistoryResponse(h.getStatus(), h.getChangedOn()))
                .toList();

        return new TicketResponse(
                t.getId(),
                t.getEmployeeName(),
                t.getEmployeeEmail(),
                t.getEmployeeMobile(),
                t.getSubject(),
                t.getMessage(),
                t.getPriority(),
                t.getStatus(),
                t.getCreatedOn(),
                t.getResolvedOn(),
                attachments,
                history
        );
    }

    public String getAttachmentViewUrl(UUID ticketId, UUID attachmentId) {
        SupportTicket ticket = ticketRepository.findById(ticketId)
                .orElseThrow(() -> new ResourceNotFoundException("Ticket not found"));

        SupportTicketAttachment attachment = ticket.getAttachments().stream()
                .filter(a -> a.getId().equals(attachmentId))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Attachment not found"));

        return storage.getSignedUrl(permanentBucket, attachment.getObjectKey());
    }
}