package com.hrstack.hr_stack.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public class SupportTicketDtos {

//What the Employee sends to the backend
    public record CreateTicketRequest(
            @NotBlank(message = "Subject is required")
            @Size(max = 255, message = "Subject cannot exceed 255 characters")
            String subject,

            @NotBlank(message = "Message is required")
            @Size(max = 2000, message = "Message cannot exceed 2000 characters")
            String message,

            @NotBlank(message = "Priority is required")
            @Pattern(regexp = "LOW|MEDIUM|HIGH|URGENT", message = "Invalid priority level")
            String priority
    ) {}

//What the Admin sees on the Dashboard
    public record TicketResponse(
            UUID id,
            String employeeName,
            String employeeEmail,
            String employeeMobile,
            String subject,
            String message,
            String priority,
            String status,
            Long createdOn,
            Long resolvedOn
    ) {}
}