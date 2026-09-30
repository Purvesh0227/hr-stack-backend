package com.hrstack.hr_stack.service;

import com.hrstack.hr_stack.dto.NotificationResponse;
import com.hrstack.hr_stack.entity.Employee;
import com.hrstack.hr_stack.entity.Notification;
import com.hrstack.hr_stack.repository.EmployeeRepository;
import com.hrstack.hr_stack.repository.NotificationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class InAppNotificationService {

    private final NotificationRepository notificationRepository;
    private final EmployeeRepository employeeRepository;

    public InAppNotificationService(
            NotificationRepository notificationRepository,
            EmployeeRepository employeeRepository) {

        this.notificationRepository = notificationRepository;
        this.employeeRepository = employeeRepository;
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

        return notificationRepository.save(notification);
    }

    @Transactional(readOnly = true)
    public List<Notification> getNotifications(String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        return notificationRepository
                .findByEmployeeOrderByCreatedOnDesc(employee);
    }


    @Transactional(readOnly = true)
    public long getUnreadCount(String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        return notificationRepository
                .countByEmployeeAndReadFalse(employee);
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
                                        "Notification not found"));

        if (!notification.getEmployee().getId()
                .equals(employee.getId())) {

            throw new RuntimeException(
                    "Notification does not belong to employee");
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

    @Transactional(readOnly = true)
    public NotificationResponse getUnreadNotificationResponse(String email) {

        Employee employee = employeeRepository.findByEmail(email)
                .orElseThrow(() ->
                        new RuntimeException("Employee not found"));

        List<Notification> notifications =
                notificationRepository
                        .findByEmployeeAndReadFalseOrderByCreatedOnDesc(employee);

        long unreadCount = notifications.size();

        return new NotificationResponse(
                notifications,
                unreadCount
        );
    }


}