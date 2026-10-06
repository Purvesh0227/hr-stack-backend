package com.hrstack.hr_stack.dto;

import java.util.UUID;

public record AttendanceResponse(
        UUID uuid,
        String empId,
        String employeeName,
        Long markedOn,
        String status
) {}