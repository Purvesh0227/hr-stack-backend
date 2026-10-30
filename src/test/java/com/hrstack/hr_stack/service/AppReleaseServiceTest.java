package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.AppReleaseDtos.AppReleaseResponse;
import com.hrstack.hr_stack.dto.AppReleaseDtos.CreateAppReleaseRequest;
import com.hrstack.hr_stack.entity.AppRelease;
import com.hrstack.hr_stack.exception.BadRequestException;
import com.hrstack.hr_stack.exception.ResourceNotFoundException;
import com.hrstack.hr_stack.repository.AppReleaseRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AppReleaseService")
class AppReleaseServiceTest {

    @Mock private AppReleaseRepository repository;
    @InjectMocks private AppReleaseService service;

    private static final String GITHUB_BASE = "https://github.com/Purvesh0227/hr-stack-frontend/releases/download/";
    private static final UUID RELEASE_ID = UUID.randomUUID();

    private static AppRelease dummyRelease(String status) {
        AppRelease r = new AppRelease();
        r.setId(RELEASE_ID);
        r.setVersion("1.0.0");
        r.setReleaseNotes("First release");
        r.setReleaseDate(LocalDate.now());
        r.setStatus(status);
        r.setPlatform("ANDROID");
        r.setDownloadUrl(GITHUB_BASE + "v1.0.0/hr-stack.apk");
        return r;
    }

    // =========================================================
    // createRelease
    // =========================================================

    @Test
    @DisplayName("createRelease: generates correct GitHub download URL and saves")
    void createRelease_success() {
        CreateAppReleaseRequest request = new CreateAppReleaseRequest(
                "1.0.1", "Bug fixes", LocalDate.now(), "RELEASED", "ANDROID"
        );

        when(repository.save(any(AppRelease.class))).thenAnswer(i -> i.getArgument(0));

        AppReleaseResponse response = service.createRelease(request);

        assertEquals("1.0.1", response.version());
        assertEquals("ANDROID", response.platform());
        assertEquals(GITHUB_BASE + "v1.0.1/hr-stack.apk", response.downloadUrl());
        verify(repository).save(any(AppRelease.class));
    }

    @Test
    @DisplayName("createRelease: WINDOWS platform builds the .exe download URL")
    void createRelease_windowsPlatform_buildsExeUrl() {
        CreateAppReleaseRequest request = new CreateAppReleaseRequest(
                "1.0.0", "Notes", LocalDate.now(), "RELEASED", "WINDOWS"
        );

        when(repository.save(any(AppRelease.class))).thenAnswer(i -> i.getArgument(0));

        AppReleaseResponse response = service.createRelease(request);

        assertEquals("WINDOWS", response.platform());
        assertEquals(
                GITHUB_BASE + "v1.0.0-desktop/HR-Stack_1.0.0_x64-setup.exe",
                response.downloadUrl());
    }

    @Test
    @DisplayName("createRelease: blank platform defaults to ANDROID")
    void createRelease_blankPlatform_defaultsToAndroid() {
        CreateAppReleaseRequest request = new CreateAppReleaseRequest(
                "1.0.0", "Notes", LocalDate.now(), "RELEASED", null
        );

        when(repository.save(any(AppRelease.class))).thenAnswer(i -> i.getArgument(0));

        AppReleaseResponse response = service.createRelease(request);

        assertEquals("ANDROID", response.platform());
        assertEquals(GITHUB_BASE + "v1.0.0/hr-stack.apk", response.downloadUrl());
    }

    @Test
    @DisplayName("createRelease: invalid platform is rejected")
    void createRelease_invalidPlatform_throws() {
        CreateAppReleaseRequest request = new CreateAppReleaseRequest(
                "1.0.0", "Notes", LocalDate.now(), "RELEASED", "IOS"
        );

        assertThrows(BadRequestException.class, () -> service.createRelease(request));
        verify(repository, never()).save(any());
    }

    @Test
    @DisplayName("createRelease: throws exception if version is blank")
    void createRelease_blankVersion_throwsException() {
        CreateAppReleaseRequest request = new CreateAppReleaseRequest(
                "  ", "Bug fixes", LocalDate.now(), "RELEASED", "ANDROID"
        );

        assertThrows(BadRequestException.class, () -> service.createRelease(request));
        verify(repository, never()).save(any());
    }

    // =========================================================
    // getReleasedForEmployee
    // =========================================================

    @Test
    @DisplayName("getReleasedForEmployee: only calls repository for RELEASED status (no filter)")
    void getReleasedForEmployee_noFilter() {
        when(repository.findByStatusOrderByReleaseDateDesc("RELEASED"))
                .thenReturn(List.of(dummyRelease("RELEASED")));

        List<AppReleaseResponse> list = service.getReleasedForEmployee(null);

        assertEquals(1, list.size());
        verify(repository).findByStatusOrderByReleaseDateDesc("RELEASED");
        verify(repository, never())
                .findByStatusAndPlatformOrderByReleaseDateDesc(anyString(), anyString());
    }

    @Test
    @DisplayName("getReleasedForEmployee: filters by platform when provided")
    void getReleasedForEmployee_withPlatform_filtered() {
        when(repository.findByStatusAndPlatformOrderByReleaseDateDesc("RELEASED", "WINDOWS"))
                .thenReturn(List.of(dummyRelease("RELEASED")));

        List<AppReleaseResponse> list = service.getReleasedForEmployee("WINDOWS");

        assertEquals(1, list.size());
        verify(repository).findByStatusAndPlatformOrderByReleaseDateDesc("RELEASED", "WINDOWS");
        verify(repository, never()).findByStatusOrderByReleaseDateDesc(anyString());
    }

    // =========================================================
    // getAllForAdmin
    // =========================================================

    @Test
    @DisplayName("getAllForAdmin: no filter returns all rows")
    void getAllForAdmin_noFilter() {
        when(repository.findAllByOrderByReleaseDateDesc())
                .thenReturn(List.of(dummyRelease("DRAFT"), dummyRelease("RELEASED")));

        List<AppReleaseResponse> list = service.getAllForAdmin(null);

        assertEquals(2, list.size());
        verify(repository).findAllByOrderByReleaseDateDesc();
        verify(repository, never()).findByPlatformOrderByReleaseDateDesc(anyString());
    }

    @Test
    @DisplayName("getAllForAdmin: filters by platform when provided")
    void getAllForAdmin_withPlatform_filtered() {
        when(repository.findByPlatformOrderByReleaseDateDesc("ANDROID"))
                .thenReturn(List.of(dummyRelease("RELEASED")));

        List<AppReleaseResponse> list = service.getAllForAdmin("ANDROID");

        assertEquals(1, list.size());
        verify(repository).findByPlatformOrderByReleaseDateDesc("ANDROID");
        verify(repository, never()).findAllByOrderByReleaseDateDesc();
    }

    // =========================================================
    // updateRelease
    // =========================================================

    @Test
    @DisplayName("updateRelease: changes version and re-generates URL")
    void updateRelease_changesUrl() {
        AppRelease existing = dummyRelease("DRAFT");
        when(repository.findById(RELEASE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(AppRelease.class))).thenAnswer(i -> i.getArgument(0));

        CreateAppReleaseRequest updateReq = new CreateAppReleaseRequest(
                "2.0.0", "Major update", LocalDate.now(), "RELEASED", "ANDROID"
        );

        AppReleaseResponse response = service.updateRelease(RELEASE_ID, updateReq);

        assertEquals("2.0.0", response.version());
        assertEquals(GITHUB_BASE + "v2.0.0/hr-stack.apk", response.downloadUrl());
        assertEquals("RELEASED", response.status());
    }

    @Test
    @DisplayName("updateRelease: switching platform to WINDOWS rebuilds the .exe URL")
    void updateRelease_switchToWindows_rebuildsExeUrl() {
        AppRelease existing = dummyRelease("RELEASED");
        when(repository.findById(RELEASE_ID)).thenReturn(Optional.of(existing));
        when(repository.save(any(AppRelease.class))).thenAnswer(i -> i.getArgument(0));

        CreateAppReleaseRequest updateReq = new CreateAppReleaseRequest(
                null, null, null, null, "WINDOWS"
        );

        AppReleaseResponse response = service.updateRelease(RELEASE_ID, updateReq);

        assertEquals("WINDOWS", response.platform());
        assertEquals(
                GITHUB_BASE + "v1.0.0-desktop/HR-Stack_1.0.0_x64-setup.exe",
                response.downloadUrl());
    }

    // =========================================================
    // deleteRelease
    // =========================================================

    @Test
    @DisplayName("deleteRelease: throws exception if ID not found")
    void deleteRelease_notFound() {
        when(repository.findById(RELEASE_ID)).thenReturn(Optional.empty());
        assertThrows(ResourceNotFoundException.class, () -> service.deleteRelease(RELEASE_ID));
    }
}