package com.hrstack.hr_stack.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.hrstack.hr_stack.dto.NotificationResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Notification;
import com.hrstack.hr_stack.entity.NotificationSubscription;
import com.hrstack.hr_stack.enums.NotificationFilter;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.NotificationRepository;
import com.hrstack.hr_stack.repository.NotificationSubscriptionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final NotificationSubscriptionRepository
            notificationSubscriptionRepository;
    private final EmployeeRepository employeeRepository;
    private final WebPushService webPushService;
    private final ObjectMapper objectMapper;

    public InAppNotificationService(
            NotificationRepository notificationRepository,
            NotificationSubscriptionRepository
                    notificationSubscriptionRepository,
            EmployeeRepository employeeRepository,
            WebPushService webPushService,
            ObjectMapper objectMapper) {

        this.notificationRepository = notificationRepository;
        this.notificationSubscriptionRepository =
                notificationSubscriptionRepository;
        this.employeeRepository = employeeRepository;
        this.webPushService = webPushService;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public Notification createNotification(
            UUID employeeId,
            String eventType,
            String message) {

        Employee employee = employeeRepository.findById(employeeId)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        Notification notification = new Notification();

        notification.setEmployee(employee);
        notification.setEmail(employee.getEmail());
        notification.setMobile(employee.getMobile());
        notification.setEventType(eventType);
        notification.setMessage(message);
        notification.setRead(false);
        notification.setCreatedOn(System.currentTimeMillis());

        Notification savedNotification =
                notificationRepository.save(notification);

        /*
         * Send Web Push notification to all registered
         * browser/device subscriptions of this employee.
         *
         * Push failure should not prevent the in-app
         * notification from being saved in the database.
         */
        sendWebPushNotifications(
                employee,
                eventType,
                message
        );

        return savedNotification;
    }

    private void sendWebPushNotifications(
            Employee employee,
            String eventType,
            String message) {

        List<NotificationSubscription> subscriptions =
                notificationSubscriptionRepository
                        .findByEmployee(employee);

        if (subscriptions.isEmpty()) {
            return;
        }

        String payload;

        try {

            Map<String, String> payloadData =
                    new HashMap<>();

            payloadData.put(
                    "eventType",
                    eventType
            );

            payloadData.put(
                    "message",
                    message
            );

            payload =
                    objectMapper.writeValueAsString(
                            payloadData
                    );

        } catch (Exception exception) {

            System.err.println(
                    "Unable to create Web Push payload: "
                            + exception.getMessage()
            );

            return;
        }

        for (NotificationSubscription subscription
                : subscriptions) {

            try {

                webPushService.sendPush(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuth(),
                        payload
                );

            } catch (Exception exception) {

                /*
                 * Push failure must not break the
                 * notification database flow.
                 */
                System.err.println(
                        "Unable to send Web Push notification: "
                                + exception.getMessage()
                );
            }
        }
    }

    @Transactional(readOnly = true)
    public NotificationResponse getNotifications(
            String email,
            NotificationFilter filter) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        List<Notification> notifications;

        switch (filter) {

            case READ:
                notifications =
                        notificationRepository
                                .findByEmployeeAndReadTrueOrderByCreatedOnDesc(
                                        employee
                                );
                break;

            case UNREAD:
                notifications =
                        notificationRepository
                                .findByEmployeeAndReadFalseOrderByCreatedOnDesc(
                                        employee
                                );
                break;

            case ALL:
            default:
                notifications =
                        notificationRepository
                                .findByEmployeeOrderByCreatedOnDesc(
                                        employee
                                );
                break;
        }

        long unreadCount =
                notificationRepository
                        .countByEmployeeAndReadFalse(employee);

        return new NotificationResponse(
                notifications,
                unreadCount
        );
    }

    @Transactional
    public void markAsRead(
            String email,
            UUID notificationId) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        Notification notification =
                notificationRepository.findById(notificationId)
                        .orElseThrow(() ->
                                new RuntimeException(
                                        "Notification not found"
                                ));

        if (!notification.getEmployee().getId()
                .equals(employee.getId())) {

            throw new RuntimeException(
                    "Notification does not belong to employee"
            );
        }

        notification.setRead(true);

        notificationRepository.save(notification);
    }

    @Transactional
    public void markAllAsRead(String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        List<Notification> notifications =
                notificationRepository
                        .findByEmployeeAndReadFalseOrderByCreatedOnDesc(
                                employee
                        );

        notifications.forEach(
                notification -> notification.setRead(true)
        );

        notificationRepository.saveAll(notifications);
    }
}