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
    private static final String GITHUB_BASE_URL =
            "https://github.com/Purvesh0227/hr-stack-frontend/releases/download/";

    private static final String PLATFORM_ANDROID = "ANDROID";
    private static final String PLATFORM_WINDOWS = "WINDOWS";

    public AppReleaseService(AppReleaseRepository repository) {
        this.repository = repository;
    }

    // =========================================================
    // READ
    // =========================================================

    public List<AppReleaseResponse> getAllForAdmin(String platform) {
        String filter = normalizeFilterPlatform(platform);

        List<AppRelease> rows = (filter == null)
                ? repository.findAllByOrderByReleaseDateDesc()
                : repository.findByPlatformOrderByReleaseDateDesc(filter);

        return rows.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    public List<AppReleaseResponse> getReleasedForEmployee(String platform) {
        String filter = normalizeFilterPlatform(platform);

        List<AppRelease> rows = (filter == null)
                ? repository.findByStatusOrderByReleaseDateDesc("RELEASED")
                : repository.findByStatusAndPlatformOrderByReleaseDateDesc("RELEASED", filter);

        return rows.stream().map(this::mapToResponse).collect(Collectors.toList());
    }

    // =========================================================
    // CREATE
    // =========================================================

    @Transactional
    public AppReleaseResponse createRelease(CreateAppReleaseRequest request) {
        if (request.version() == null || request.version().isBlank()) {
            throw new BadRequestException("Version is required");
        }

        String platform = normalizePlatform(request.platform());

        long now = System.currentTimeMillis();

        AppRelease release = new AppRelease();
        release.setVersion(request.version());
        release.setReleaseNotes(request.releaseNotes());
        release.setReleaseDate(request.releaseDate());
        release.setStatus(request.status());
        release.setPlatform(platform);
        release.setDownloadUrl(buildDownloadUrl(request.version(), platform));
        release.setCreatedOn(now);
        release.setUpdatedOn(now);

        return mapToResponse(repository.save(release));
    }

    // =========================================================
    // UPDATE
    // =========================================================

    @Transactional
    public AppReleaseResponse updateRelease(UUID id, CreateAppReleaseRequest request) {
        AppRelease release = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found"));

        if (request.version() != null) {
            release.setVersion(request.version());
        }
        if (request.platform() != null) {
            release.setPlatform(normalizePlatform(request.platform()));
        }
        if (request.releaseNotes() != null) release.setReleaseNotes(request.releaseNotes());
        if (request.releaseDate() != null) release.setReleaseDate(request.releaseDate());
        if (request.status() != null) release.setStatus(request.status());

        // Always rebuild the URL so it stays in sync with version + platform
        release.setDownloadUrl(
                buildDownloadUrl(release.getVersion(), release.getPlatform()));

        release.setUpdatedOn(System.currentTimeMillis());

        return mapToResponse(repository.save(release));
    }

    // =========================================================
    // DELETE
    // =========================================================

    @Transactional
    public void deleteRelease(UUID id) {
        AppRelease release = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Release not found"));
        repository.delete(release);
    }

    // =========================================================
    // HELPERS
    // =========================================================

    /** Build the GitHub download URL based on platform. */
    private String buildDownloadUrl(String version, String platform) {
        String tag = "v" + version;

        if (PLATFORM_WINDOWS.equalsIgnoreCase(platform)) {
            // Notice the -desktop added to the tag!
            return GITHUB_BASE_URL + tag + "-desktop"
                    + "/HR-Stack_" + version + "_x64-setup.exe";
        }

        // Default: Android APK
        return GITHUB_BASE_URL + tag + "/hr-stack.apk";
    }

    /** Normalizes a platform value from a request body. Defaults to ANDROID. */
    private String normalizePlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return PLATFORM_ANDROID;
        }
        String p = platform.trim().toUpperCase();
        if (!p.equals(PLATFORM_ANDROID) && !p.equals(PLATFORM_WINDOWS)) {
            throw new BadRequestException(
                    "Invalid platform. Use ANDROID or WINDOWS.");
        }
        return p;
    }

    /** For filtering: blank/null means "no filter" (return all). */
    private String normalizeFilterPlatform(String platform) {
        if (platform == null || platform.isBlank()) {
            return null;
        }
        return normalizePlatform(platform);
    }

    private AppReleaseResponse mapToResponse(AppRelease release) {
        return new AppReleaseResponse(
                release.getId(),
                release.getVersion(),
                release.getReleaseNotes(),
                release.getReleaseDate(),
                release.getStatus(),
                release.getPlatform(),
                release.getDownloadUrl()
        );
    }
}