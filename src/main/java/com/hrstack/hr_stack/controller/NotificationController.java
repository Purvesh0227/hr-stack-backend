package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.NotificationResponse;
import com.hrstack.hr_stack.enums.NotificationFilter;
import com.hrstack.hr_stack.service.InAppNotificationService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/employee/notifications")
@Hidden
@SecurityRequirement(name = "bearerAuth")
public class NotificationController {

    private final InAppNotificationService notificationService;

    public NotificationController(
            InAppNotificationService notificationService) {

        this.notificationService = notificationService;
    }

    @GetMapping
    public ResponseEntity<NotificationResponse> getNotifications(
            Authentication authentication,
            @RequestParam(
                    name = "status",
                    defaultValue = "UNREAD"
            ) NotificationFilter status) {

        return ResponseEntity.ok(
                notificationService.getNotifications(
                        authentication.getName(),
                        status
                )
        );
    }

    @PatchMapping("/{notificationId}/read")
    public ResponseEntity<Void> markAsRead(
            Authentication authentication,
            @PathVariable UUID notificationId) {

        notificationService.markAsRead(
                authentication.getName(),
                notificationId
        );

        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/read-all")
    public ResponseEntity<Void> markAllAsRead(
            Authentication authentication) {

        notificationService.markAllAsRead(
                authentication.getName()
        );

        return ResponseEntity.noContent().build();
    }
}