package com.hrstack.hr_stack.dto;

import java.time.LocalDate;
import java.util.UUID;

public class AppReleaseDtos {

    public record AppReleaseResponse(
            UUID id,
            String version,
            String releaseNotes,
            LocalDate releaseDate,
            String status,
            String downloadUrl
    ) {}

    public record CreateAppReleaseRequest(
            String version,
            String releaseNotes,
            LocalDate releaseDate,
            String status
    ) {}
}