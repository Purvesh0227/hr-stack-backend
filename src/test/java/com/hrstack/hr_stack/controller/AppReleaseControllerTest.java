package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.AppReleaseDtos.AppReleaseResponse;
import com.hrstack.hr_stack.service.AppReleaseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@DisplayName("AppReleaseController Security & API")
class AppReleaseControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AppReleaseService service;

    private final UUID RELEASE_ID = UUID.randomUUID();
    private final AppReleaseResponse mockResponse = new AppReleaseResponse(
            RELEASE_ID, "1.0.0", "Notes", LocalDate.now(),
            "RELEASED", "ANDROID", "http://github.com"
    );

    @Test
    @DisplayName("Employee can GET the public list (200)")
    void employeeCanGetPublicList() throws Exception {
        when(service.getReleasedForEmployee(any())).thenReturn(List.of(mockResponse));

        mockMvc.perform(get("/employee/app-releases/public")
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].version").value("1.0.0"))
                .andExpect(jsonPath("$[0].platform").value("ANDROID"));
    }

    @Test
    @DisplayName("Public list accepts ?platform=WINDOWS and passes it to the service")
    void publicList_platformParam_passedToService() throws Exception {
        when(service.getReleasedForEmployee("WINDOWS")).thenReturn(List.of());

        mockMvc.perform(get("/employee/app-releases/public")
                        .param("platform", "WINDOWS")
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isOk());

        verify(service).getReleasedForEmployee("WINDOWS");
    }

    @Test
    @DisplayName("Admin GET /admin accepts ?platform= and passes it to the service")
    void adminList_platformParam_passedToService() throws Exception {
        when(service.getAllForAdmin("WINDOWS")).thenReturn(List.of());

        mockMvc.perform(get("/employee/app-releases/admin")
                        .param("platform", "WINDOWS")
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk());

        verify(service).getAllForAdmin("WINDOWS");
    }

    @Test
    @DisplayName("Employee CANNOT POST a new release (403)")
    void employeeCannotPost() throws Exception {
        mockMvc.perform(post("/employee/app-releases")
                        .contentType(APPLICATION_JSON)
                        .content("{}")
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Employee CANNOT DELETE a release (403)")
    void employeeCannotDelete() throws Exception {
        mockMvc.perform(delete("/employee/app-releases/" + RELEASE_ID)
                        .with(user("emp@example.com").roles("EMPLOYEE")))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("Admin can POST a new release (200)")
    void adminCanPost() throws Exception {
        when(service.createRelease(any())).thenReturn(mockResponse);

        mockMvc.perform(post("/employee/app-releases")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {
                                  "version": "1.0.0",
                                  "releaseNotes": "Notes",
                                  "releaseDate": "2026-10-27",
                                  "status": "RELEASED",
                                  "platform": "ANDROID"
                                }
                                """)
                        .with(user("admin@example.com").roles("ADMIN")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.downloadUrl").exists())
                .andExpect(jsonPath("$.platform").value("ANDROID"));
    }
}