package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.SupportTicketDtos.CreateTicketRequest;
import com.hrstack.hr_stack.dto.SupportTicketDtos.TicketResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.SupportTicket;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.SupportTicketRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
public class SupportTicketService {

    private final SupportTicketRepository ticketRepository;
    private final EmployeeRepository employeeRepository;
    private final EmailService emailService;
    private final InAppNotificationService inAppNotificationService;

    @Value("${app.support.email}")
    private String supportEmail;

    public SupportTicketService(
            SupportTicketRepository ticketRepository,
            EmployeeRepository employeeRepository,
            EmailService emailService,
            InAppNotificationService inAppNotificationService) {
        this.ticketRepository = ticketRepository;
        this.employeeRepository = employeeRepository;
        this.emailService = emailService;
        this.inAppNotificationService = inAppNotificationService;
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
                t.getResolvedOn()
        );
    }
}