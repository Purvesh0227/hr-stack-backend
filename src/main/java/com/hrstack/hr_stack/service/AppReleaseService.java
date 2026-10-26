package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AppReleaseDtos.AppReleaseResponse;
import com.hrstack.hr_stack.dto.AppReleaseDtos.CreateAppReleaseRequest;
import com.hrstack.hr_stack.entity.AppRelease;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AppReleaseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class AppReleaseService {

    private final AppReleaseRepository repository;

    // Your specific GitHub base URL
    private static final String GITHUB_BASE_URL = "https://github.com/Purvesh0227/hr-stack-frontend/releases/download/";

    public AppReleaseService(AppReleaseRepository repository) {
        this.repository = repository;
    }

    public List<AppReleaseResponse> getAllForAdmin() {
        return repository.findAllByOrderByReleaseDateDesc()
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AppReleaseResponse> getReleasedForEmployee() {
        return repository.findByStatusOrderByReleaseDateDesc("RELEASED")
                .stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    @Transactional
    public AppReleaseResponse createRelease(CreateAppReleaseRequest request) {
        if (request.version() == null || request.version().isBlank()) {
            throw new BadRequestException("Version is required");
        }

        // Automatically generate the link (e.g., .../download/v1.0.0/hr-stack.apk)
        String downloadUrl = GITHUB_BASE_URL + "v" + request.version() + "/hr-stack.apk";

        long now = System.currentTimeMillis();

        AppRelease release = new AppRelease();
        release.setVersion(request.version());
        release.setReleaseNotes(request.releaseNotes());
        release.setReleaseDate(request.releaseDate());
        release.setStatus(request.status());
        release.setDownloadUrl(downloadUrl);
        release.setCreatedOn(now);
        release.setUpdatedOn(now);

        return mapToResponse(repository.save(release));
    }

    @Transactional
    public AppReleaseResponse updateRelease(UUID id, CreateAppReleaseRequest request) {
        AppRelease release = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found"));

        if (request.version() != null) {
            release.setVersion(request.version());
            // Re-generate URL if version changed
            release.setDownloadUrl(GITHUB_BASE_URL + "v" + request.version() + "/hr-stack.apk");
        }
        if (request.releaseNotes() != null) release.setReleaseNotes(request.releaseNotes());
        if (request.releaseDate() != null) release.setReleaseDate(request.releaseDate());
        if (request.status() != null) release.setStatus(request.status());

        release.setUpdatedOn(System.currentTimeMillis());
        return mapToResponse(repository.save(release));
    }

    @Transactional
    public void deleteRelease(UUID id) {
        AppRelease release = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found"));
        repository.delete(release);
    }

    private AppReleaseResponse mapToResponse(AppRelease release) {
        return new AppReleaseResponse(
                release.getId(),
                release.getVersion(),
                release.getReleaseNotes(),
                release.getReleaseDate(),
                release.getStatus(),
                release.getDownloadUrl()
        );
    }
}