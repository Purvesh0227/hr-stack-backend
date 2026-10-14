package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface AnnouncementRepository extends JpaRepository<Announcement, UUID> {

    Page<Announcement> findByTypeIgnoreCase(String type,Pageable pageable);
}