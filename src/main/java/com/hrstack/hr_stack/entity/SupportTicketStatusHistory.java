package com.hrstack.hr_stack.entity;

import jakarta.persistence.*;
import java.util.UUID;

@Entity
@Table(name = "support_ticket_status_history")
public class SupportTicketStatusHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "ticket_id", nullable = false)
    private SupportTicket ticket;

    @Column(nullable = false, length = 50)
    private String status;

    @Column(name = "changed_on", nullable = false)
    private Long changedOn;

    public SupportTicketStatusHistory() {}

    public UUID getId() { return id; }
    public SupportTicket getTicket() { return ticket; }
    public void setTicket(SupportTicket ticket) { this.ticket = ticket; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public Long getChangedOn() { return changedOn; }
    public void setChangedOn(Long changedOn) { this.changedOn = changedOn; }
}