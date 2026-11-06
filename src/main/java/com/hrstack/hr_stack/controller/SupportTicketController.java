package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.PageResponse;
import com.hrstack.hr_stack.dto.SupportTicketDtos.CreateTicketRequest;
import com.hrstack.hr_stack.dto.SupportTicketDtos.TicketResponse;
import com.hrstack.hr_stack.service.SupportTicketService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/employee/support")
@Tag(name = "Support Tickets")
@SecurityRequirement(name = "bearerAuth")
public class SupportTicketController {

    private final SupportTicketService supportTicketService;

    public SupportTicketController(SupportTicketService supportTicketService) {
        this.supportTicketService = supportTicketService;
    }

    @PostMapping
    public ResponseEntity<Map<String, String>> createTicket(
            Authentication authentication,
            @Valid @RequestBody CreateTicketRequest request) {

        supportTicketService.createTicket(authentication.getName(), request);

        return ResponseEntity.ok(Map.of(
                "message", "Ticket submitted successfully. Our team will contact you shortly."
        ));
    }

    @GetMapping("/my-tickets")
    public ResponseEntity<PageResponse<TicketResponse>> getMyTickets(
            Authentication authentication,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "5") int size) {

        return ResponseEntity.ok(
                PageResponse.from(supportTicketService.getMyTickets(authentication.getName(), page, size))
        );
    }

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<PageResponse<TicketResponse>> getAdminTickets(
            @RequestParam(defaultValue = "ALL") String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {

        return ResponseEntity.ok(
                PageResponse.from(supportTicketService.getTickets(status, page, size))
        );
    }

    @PatchMapping("/admin/{ticketId}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable UUID ticketId,
            @RequestBody Map<String, String> request) {

        String newStatus = request.get("status");
        if (newStatus == null) {
            return ResponseEntity.badRequest().body(Map.of("error", "Status is required"));
        }

        supportTicketService.updateTicketStatus(ticketId, newStatus);

        return ResponseEntity.ok(Map.of(
                "message", "Ticket status updated to " + newStatus
        ));
    }
}