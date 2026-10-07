package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.ShortLink;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ShortLinkRepository extends JpaRepository<ShortLink, UUID> {

    Optional<ShortLink> findByShortCode(String shortCode);

}