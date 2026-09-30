package com.hrstack.hr_stack.dto;

import com.hrstack.hr_stack.entity.Notification;

import java.util.List;

public class NotificationResponse {

    private List<Notification> notifications;
    private long unreadCount;

    public NotificationResponse(
            List<Notification> notifications,
            long unreadCount) {

        this.notifications = notifications;
        this.unreadCount = unreadCount;
    }

    public List<Notification> getNotifications() {
        return notifications;
    }

    public long getUnreadCount() {
        return unreadCount;
    }
}