package com.hrstack.hr_stack.repository;

import com.hrstack.hr_stack.entity.AppRelease;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AppReleaseRepository extends JpaRepository<AppRelease, UUID> {
    List<AppRelease> findAllByOrderByReleaseDateDesc();
    List<AppRelease> findByStatusOrderByReleaseDateDesc(String status);

    List<AppRelease> findByPlatformOrderByReleaseDateDesc(String platform);

    List<AppRelease> findByStatusAndPlatformOrderByReleaseDateDesc(String status, String platform);
}