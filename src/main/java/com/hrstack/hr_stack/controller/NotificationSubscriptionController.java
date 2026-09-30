package com.hrstack.hr_stack.controller;

import com.hrstack.hr_stack.dto.PushSubscriptionRequest;
import com.hrstack.hr_stack.service.NotificationSubscriptionService;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/employee/notifications")
@SecurityRequirement(name = "bearerAuth")
@Hidden
public class NotificationSubscriptionController {
    private final NotificationSubscriptionService subscriptionService;

    public NotificationSubscriptionController(NotificationSubscriptionService subscriptionService) {
        this.subscriptionService = subscriptionService;
    }

    @PostMapping("/subscribe")
    public ResponseEntity<Void> subscriber(
            Authentication authentication,
            @RequestBody PushSubscriptionRequest request){
                subscriptionService.subscribe(
                        authentication.getName(),
                        request
                );
                return ResponseEntity.ok().build();
    }

    @DeleteMapping("/unsubscribe")
    public ResponseEntity<Void> unsubscribe(
            Authentication authentication,
            @RequestBody PushSubscriptionRequest request) {

        subscriptionService.unsubscribe(
                authentication.getName(),
                request.getEndpoint()
        );

        return ResponseEntity.noContent().build();
    }
}
