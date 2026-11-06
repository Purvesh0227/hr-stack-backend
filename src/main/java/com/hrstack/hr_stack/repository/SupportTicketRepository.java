package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.SupportTicket;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SupportTicketRepository extends JpaRepository<SupportTicket, UUID> {
// Fetch tickets by status and sort by newest first
    Page<SupportTicket> findByStatusOrderByCreatedOnDesc(String status, Pageable pageable);

// Fetch all tickets sort by new first
    Page<SupportTicket> findAllByOrderByCreatedOnDesc(Pageable pageable);

// Fetch tickets for a specific employee
    Page<SupportTicket> findByEmployeeIdOrderByCreatedOnDesc(UUID employeeId, Pageable pageable);
}